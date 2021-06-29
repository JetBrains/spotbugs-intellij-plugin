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

package org.jetbrains.plugins.spotbugs.steps;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.utils.Keyboard;
import kotlin.Unit;
import org.jetbrains.plugins.spotbugs.pages.DialogFixture;
import org.jetbrains.plugins.spotbugs.pages.IdeaFrame;
import org.jetbrains.plugins.spotbugs.pages.WelcomeFrameFixture;

import java.awt.event.KeyEvent;
import java.time.Duration;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;

public class JavaExampleSteps {
    final private RemoteRobot remoteRobot;
    final private Keyboard keyboard;

    public JavaExampleSteps(RemoteRobot remoteRobot) {
        this.remoteRobot = remoteRobot;
        this.keyboard = new Keyboard(remoteRobot);
    }

    public void createNewCommandLineProject() {
        step("Create New Command Line Project", () -> {
            final WelcomeFrameFixture welcomeFrame = remoteRobot.find(WelcomeFrameFixture.class);
            welcomeFrame.createNewProjectLink().click();

            final DialogFixture newProjectDialog = welcomeFrame.find(DialogFixture.class, DialogFixture.byTitle("New Project"), Duration.ofSeconds(20));
            newProjectDialog.findText("Java").click();
            newProjectDialog.find(ComponentFixture.class,
                    byXpath("FrameworksTree", "//div[@class='FrameworksTree']"))
                    .findText("Kotlin/JVM")
                    .click();
            keyboard.key(KeyEvent.VK_SPACE, Duration.ZERO);
            newProjectDialog.button("Next").click();
            newProjectDialog.button("Finish").click();
        });
    }

    public void closeTipOfTheDay() {
        step("Close Tip of the Day if it appears", () -> {
            final IdeaFrame idea = remoteRobot.find(IdeaFrame.class);
            idea.dumbAware(() -> {
                try {
                    idea.find(DialogFixture.class, DialogFixture.byTitle("Tip of the Day")).button("Close").click();
                } catch (Throwable ignore) {
                }
                return Unit.INSTANCE;
            });
        });
    }
}
