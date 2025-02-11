/*
 * Copyright (c) 2021-2022 Osiris-Team.
 * All rights reserved.
 *
 * This software is copyrighted work, licensed under the terms
 * of the MIT-License. Consult the "LICENSE" file for details.
 */

package com.osiris.autoplug.client.network.online.connections;

import com.osiris.autoplug.client.Server;
import com.osiris.autoplug.client.configs.WebConfig;
import com.osiris.autoplug.client.console.AutoPlugConsole;
import com.osiris.autoplug.client.network.online.SecondaryConnection;
import com.osiris.autoplug.core.logger.AL;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * The user can send commands through the online console.<br>
 * For that we got this connection, which listens for the user
 * input at the online console and executes it.
 */
public class ConOnlineConsoleReceive extends SecondaryConnection {
    @Nullable
    private static Thread thread;

    public ConOnlineConsoleReceive() {
        super((byte) 1);
    }

    @Override
    public boolean open() throws Exception {
        if (new WebConfig().online_console.asBoolean()) {
            super.open();
            if (thread != null && (thread.isAlive() || !thread.isInterrupted()))
                thread.interrupt();
            thread = new Thread(() -> {
                try {
                    Socket socket = getSocket();
                    socket.setSoTimeout(0);
                    InputStream in = getSocket().getInputStream();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                        String line;
                        while (!socket.isClosed() && (line = reader.readLine()) != null) {
                            AL.info("Received Web-Command: " + line);
                            if (!AutoPlugConsole.executeCommand(line))
                                Server.submitCommand(line);
                        }
                    }
                } catch (Exception e) {
                    AL.warn(this.getClass(), e);
                }

            });
            thread.start();
            AL.debug(this.getClass(), "Connection '" + this.getClass().getSimpleName() + "' connected.");
            return true;
        } else {
            AL.debug(this.getClass(), "Connection '" + this.getClass().getSimpleName() + "' not connected, because not enabled in the web-config.");
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (thread != null && !thread.isInterrupted()) thread.interrupt();
        } catch (Exception e) {
            AL.warn("Failed to stop thread.", e);
        }
        thread = null;

        try {
            super.close();
        } catch (Exception e) {
            AL.warn("Failed to close connection.", e);
        }
    }
}
