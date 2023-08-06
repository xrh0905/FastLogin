/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2023 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.velocity.task;

import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.FloodgateManagement;
import com.github.games647.fastlogin.velocity.FastLoginVelocity;
import com.github.games647.fastlogin.velocity.VelocityLoginSession;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FloodgateAuthTask
        extends FloodgateManagement<Player, CommandSource, VelocityLoginSession, FastLoginVelocity> {

    private final RegisteredServer server;

    public FloodgateAuthTask(FastLoginCore<Player, CommandSource, FastLoginVelocity> core, Player player,
                             FloodgatePlayer floodgatePlayer, RegisteredServer server) {
        super(core, player, floodgatePlayer);
        this.server = server;
    }

    @Override
    protected void startLogin() {
        VelocityLoginSession session = new VelocityLoginSession(player.getUsername(), isRegistered, profile);
        core.getPlugin().getSession().put(player.getRemoteAddress(), session);

        // enable auto login based on the value of 'autoLoginFloodgate' in config.yml
        boolean forcedOnlineMode = autoLoginFloodgate.equals("true")
                || (autoLoginFloodgate.equals("linked") && isLinked);

        core.getPlugin().getLog()
                .info("Processing player {} with OnlineMode {}", player.getUsername(), forcedOnlineMode);

        // delay sending force command, because Paper will process the login event asynchronously
        // In this case it means that the force command (plugin message) is already received and processed while
        // player is still in the login phase and reported to be offline.
        Runnable loginTask = new ForceLoginTask(core.getPlugin().getCore(), player, server, session, forcedOnlineMode);
        core.getPlugin().getProxy().getScheduler()
                .buildTask(core.getPlugin(), () -> core.getPlugin().getScheduler().runAsync(loginTask))
                .delay(2L, TimeUnit.SECONDS) // Delay at least one second, otherwise the login command can be missed
                .schedule();
    }

    @Override
    protected String getName(Player player) {
        return player.getUsername();
    }

    @Override
    protected UUID getUUID(Player player) {
        return player.getUniqueId();
    }

    @Override
    protected InetSocketAddress getAddress(Player player) {
        return player.getRemoteAddress();
    }

}
