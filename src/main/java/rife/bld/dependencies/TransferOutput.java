/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.ArrayList;
import java.util.List;

/**
 * Outputs the status of artifact transfers to the console.
 * <p>
 * When the console is an interactive terminal, the transfers that are in
 * progress are displayed in a live updating block at the bottom of the
 * output, with each transfer reporting its progress as it happens.
 * Otherwise, a single line is printed when each transfer finishes, keeping
 * the output stable for CI logs, pipes and IDE consoles, while still never
 * interleaving the lines of parallel transfers.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.3.1
 */
abstract class TransferOutput {
    private static final TransferOutput INSTANCE = create();

    static TransferOutput instance() {
        return INSTANCE;
    }

    private static TransferOutput create() {
        if (System.console() != null && !"dumb".equals(System.getenv("TERM"))) {
            return new AnsiTransferOutput();
        }
        return new PlainTransferOutput();
    }

    /**
     * Starts tracking a single artifact transfer.
     *
     * @param location the location of the artifact that is being transferred
     * @return the transfer to report progress and completion on
     * @since 2.3.1
     */
    abstract Transfer start(String location);

    /**
     * A single artifact transfer whose progress is being tracked.
     *
     * @since 2.3.1
     */
    abstract static class Transfer {
        protected final String location_;

        protected Transfer(String location) {
            location_ = location;
        }

        /**
         * Reports transfer progress.
         *
         * @param transferred the number of bytes transferred so far
         * @param total       the total number of bytes; or {@code -1} when unknown
         * @since 2.3.1
         */
        abstract void progress(long transferred, long total);

        /**
         * Reports that the transfer finished.
         *
         * @param status the final status of the transfer, like
         *               {@code done}, {@code exists} or {@code not found}
         * @since 2.3.1
         */
        abstract void finish(String status);
    }

    static String describe(String location, String status) {
        return "Downloading: " + location + " ... " + status;
    }

    private static class PlainTransferOutput extends TransferOutput {
        Transfer start(String location) {
            return new Transfer(location) {
                void progress(long transferred, long total) {
                    // no-op, only the finished state is printed
                }

                void finish(String status) {
                    System.out.println(describe(location_, status));
                }
            };
        }
    }

    private static class AnsiTransferOutput extends TransferOutput {
        private static final long REPAINT_INTERVAL_MS = 100;
        private static final int MAX_LINE_WIDTH = 79;

        private final List<AnsiTransfer> active_ = new ArrayList<>();
        private int paintedLines_ = 0;
        private long lastRepaint_ = 0;

        synchronized Transfer start(String location) {
            var transfer = new AnsiTransfer(location);
            active_.add(transfer);
            repaint(null);
            return transfer;
        }

        private synchronized void reportProgress() {
            if (System.currentTimeMillis() - lastRepaint_ >= REPAINT_INTERVAL_MS) {
                repaint(null);
            }
        }

        private synchronized void reportFinished(AnsiTransfer transfer, String status) {
            active_.remove(transfer);
            repaint(describe(transfer.location_, status));
        }

        private void repaint(String finishedLine) {
            var output = new StringBuilder();
            if (paintedLines_ > 0) {
                // move the cursor to the beginning of the live block
                // and clear everything below it
                output.append("\u001B[").append(paintedLines_).append("F\u001B[0J");
            }
            if (finishedLine != null) {
                output.append(finishedLine).append('\n');
            }
            for (var transfer : active_) {
                output.append(transfer.statusLine()).append('\n');
            }
            paintedLines_ = active_.size();
            lastRepaint_ = System.currentTimeMillis();
            System.out.print(output);
            System.out.flush();
        }

        private class AnsiTransfer extends Transfer {
            private volatile long transferred_ = 0;
            private volatile long total_ = -1;

            AnsiTransfer(String location) {
                super(location);
            }

            void progress(long transferred, long total) {
                transferred_ = transferred;
                total_ = total;
                reportProgress();
            }

            void finish(String status) {
                reportFinished(this, status);
            }

            String statusLine() {
                var filename = location_.substring(location_.lastIndexOf('/') + 1);
                var line = new StringBuilder("Downloading: ").append(filename).append(" ... ");
                if (total_ > 0) {
                    line.append(transferred_ * 100 / total_).append('%');
                } else if (transferred_ > 0) {
                    line.append(humanBytes(transferred_));
                }
                // prevent lines from wrapping since that would break
                // the cursor repositioning of the repaints
                if (line.length() > MAX_LINE_WIDTH) {
                    line.setLength(MAX_LINE_WIDTH);
                }
                return line.toString();
            }

            private String humanBytes(long bytes) {
                if (bytes >= 1024 * 1024) {
                    return (bytes / (1024 * 1024)) + " MB";
                }
                if (bytes >= 1024) {
                    return (bytes / 1024) + " KB";
                }
                return bytes + " B";
            }
        }
    }
}
