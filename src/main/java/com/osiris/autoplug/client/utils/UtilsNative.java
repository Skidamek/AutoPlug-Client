/*
 * Copyright (c) 2022 Osiris-Team.
 * All rights reserved.
 *
 * This software is copyrighted work, licensed under the terms
 * of the MIT-License. Consult the "LICENSE" file for details.
 */

package com.osiris.autoplug.client.utils;

import com.osiris.autoplug.client.configs.SystemConfig;
import org.jline.utils.OSUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UtilsNative {
    private final String serviceName = "AutoPlug";

    public void enableStartOnBootIfNeeded(File jar) throws IOException, InterruptedException {
        if (!isRegistered()) register(jar);
    }

    public void disableStartOnBootIfNeeded() throws IOException, InterruptedException {
        if (isRegistered()) remove();
    }

    public boolean isRegistered() {
        try {
            return new SystemConfig().is_autostart_registered.asBoolean();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Registers platform specific stuff
     * to launch the provided jar at system boot,
     * once the user is logged in.
     * Windows and UNIX like platforms supported.
     */
    public void register(File jar) throws IOException, InterruptedException {
        File startScript = new File(GD.WORKING_DIR + "/autoplug/system/" + serviceName + (OSUtils.IS_WINDOWS ? ".bat" : ".sh"));
        if (!startScript.exists()) {
            startScript.getParentFile().mkdirs();
            startScript.createNewFile();
        }
        Files.write(startScript.toPath(), ("" +
                "echo " + "     ___       __       ___  __             \n" +
                "echo " + "    / _ |__ __/ /____  / _ \\/ /_ _____ _   \n" +
                "echo " + "   / __ / // / __/ _ \\/ ___/ / // / _ `/   \n" +
                "echo " + "  /_/ |_\\_,_/\\__/\\___/_/  /_/\\_,_/\\_, /\n" +
                "echo " + "                                 /___/      \n" +
                "echo No need to worry about this window, its just AutoPlug starting automatically in the background.\n" +
                "echo You should be able to access the terminal over the system-tray.\n" +
                "echo To abort enter CTRL+C.\n" +
                "echo This window will disappear in 10 seconds.\n" +
                (OSUtils.IS_WINDOWS ? "timeout /t 10 /nobreak\n" : "sleep 10\n") +
                "cd \"" + jar.getParentFile().getAbsolutePath() + "\"\n" +
                "javaw -jar \"" + jar.getAbsolutePath() + "\"\n" + // javaw to start without terminal
                "").getBytes(StandardCharsets.UTF_8));
        if (OSUtils.IS_WINDOWS) {
            Process p = new ProcessBuilder().command("REG",
                    "ADD", "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/V", serviceName, "/t", "REG_SZ", "/F", "/D", ("\"" + startScript.getAbsolutePath() + "\"")).start();
            // The name AutoPlug doesnt get set on Win10,
            // but the file name is used, thus we create the AutoPlug.bat
            while (p.isAlive()) Thread.sleep(100);
            if (p.exitValue() != 0) {
                throw new IOException("Failed to register AutoPlug start on boot in Windows registry (error code: " + p.exitValue() + ")." +
                        " Error stream: \n" + new Streams().read(p.getErrorStream()) + " Regular stream: \n" + new Streams().read(p.getInputStream()));
            }

        } else { // UNIX STUFF
            File userHomeDir = new File(System.getProperty("user.home"));

            // Create and write service file:
            File serviceFile = new File(userHomeDir + "/.config/systemd/user/" + serviceName + ".service");
            serviceFile.getParentFile().mkdirs();
            serviceFile.delete();
            serviceFile.createNewFile();
            if (!serviceFile.exists())
                throw new IOException("Failed to create required service file at " + serviceFile);
            Files.write(serviceFile.toPath(), (
                    "[Unit]\n" +
                            "Description=Starts AutoPlug on system boot.\n" +
                            "\n" +
                            "[Service]\n" +
                            "ExecStart=\"" + startScript + "\"\n" +
                            "\n" +
                            "[Install]\n" +
                            "WantedBy=default.target\n").getBytes(StandardCharsets.UTF_8));
            if (serviceFile.length() == 0) throw new IOException("Size of service file cannot be 0! " + serviceFile);

            Process restartSystemCtl = new ProcessBuilder().command("systemctl", "--user", "daemon-reload").start();
            while (restartSystemCtl.isAlive()) Thread.sleep(100); // Wait until finishes

            Process checkIfRegistered = new ProcessBuilder().command("systemctl", "--user", "list-unit-files", serviceFile.getName()).start();
            while (checkIfRegistered.isAlive()) Thread.sleep(100); // Wait until finishes
            String out = new Streams().read(checkIfRegistered.getInputStream());
            if (checkIfRegistered.exitValue() != 0 || !out.contains(serviceFile.getName()))
                throw new IOException("Failed to register service file. ERROR_STREAM(" + checkIfRegistered.exitValue() + "): " + new Streams().read(checkIfRegistered.getErrorStream())
                        + " OUTPUT: " + out);
        }

        try {
            SystemConfig systemConfig = new SystemConfig();
            systemConfig.lockFile();
            systemConfig.is_autostart_registered.setValues("true");
            systemConfig.save();
            systemConfig.unlockFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void remove() throws IOException, InterruptedException {
        if (OSUtils.IS_WINDOWS) {
            Process p = new ProcessBuilder().command("REG",
                    "DELETE", "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/V", serviceName, "/F").start();
            while (p.isAlive()) Thread.sleep(100);
            if (p.exitValue() != 0) {
                throw new IOException("Failed to unregister AutoPlug start on boot in Windows registry (error code: " + p.exitValue() + ")." +
                        " Error stream: \n" + new Streams().read(p.getErrorStream()) + " Regular stream: \n" + new Streams().read(p.getInputStream()));
            }

        } else { // UNIX STUFF
            File userHomeDir = new File(System.getProperty("user.home"));
            File serviceFile = new File(userHomeDir + "/.config/systemd/user/" + serviceName + ".service");
            serviceFile.delete();
            if (serviceFile.exists())
                throw new IOException("Failed to delete service file at " + serviceFile);

            Process restartSystemCtl = new ProcessBuilder().command("systemctl", "--user", "daemon-reload").start();
            while (restartSystemCtl.isAlive()) Thread.sleep(100); // Wait until finishes
        }
        try {
            SystemConfig systemConfig = new SystemConfig();
            systemConfig.lockFile();
            systemConfig.is_autostart_registered.setValues("false");
            systemConfig.save();
            systemConfig.unlockFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
