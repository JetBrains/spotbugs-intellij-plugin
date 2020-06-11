/*
 * Copyright 2020 Reshift Security Intellij plugin contributors
 *
 * This file is part of Reshift Security Intellij plugin.
 *
 * Reshift Security Intellij plugin is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Reshift Security Intellij plugin is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Reshift Security Intellij plugin.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.reshiftsecurity.analytics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsService {
    List<AnalyticsAction> actions;
    Gson jsonSerializer;

    public AnalyticsService() {
        this.actions = new ArrayList<>();
        this.jsonSerializer = new GsonBuilder().setPrettyPrinting().create();
    }

    public void recordAction(AnalyticsAction action) {
        this.actions.add(action);
        this.processActions();
    }

    private void processActions() {
        if (actions.size() >= 100) {
            new Thread(() -> {
                // TODO: send actions async and reset
                String actionsJson = this.jsonSerializer.toJson(this.actions);
                this.actions = new ArrayList<>();
            }).start();
        }
    }
}