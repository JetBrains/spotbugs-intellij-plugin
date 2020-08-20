/*
 * Copyright 2020 SpotBugs plugin contributors
 *
 * This file is part of IntelliJ SpotBugs plugin.
 *
 * IntelliJ SpotBugs plugin is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * IntelliJ SpotBugs plugin is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IntelliJ SpotBugs plugin.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.spotbugs.actions;

import com.intellij.openapi.diagnostic.Logger;
import edu.umd.cs.findbugs.BugCollection;
import org.jetbrains.annotations.NotNull;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;

public interface BugCollectionExporter {
    Logger LOGGER = Logger.getInstance(ExportBugCollection.class);

    void export(@NotNull final BugCollection bugCollection, @NotNull final File file) throws IOException, TransformerException;
}
