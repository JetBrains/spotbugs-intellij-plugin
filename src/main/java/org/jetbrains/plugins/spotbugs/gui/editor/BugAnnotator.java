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
package org.jetbrains.plugins.spotbugs.gui.editor;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Detector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.spotbugs.common.ExtendedProblemDescriptor;
import org.jetbrains.plugins.spotbugs.common.util.BugInstanceUtil;
import org.jetbrains.plugins.spotbugs.common.util.StringUtilFb;
import org.jetbrains.plugins.spotbugs.core.FindBugsState;
import org.jetbrains.plugins.spotbugs.core.ProblemCacheService;
import org.jetbrains.plugins.spotbugs.core.WorkspaceSettings;
import org.jetbrains.plugins.spotbugs.intentions.ClearAndSuppressBugIntentionAction;
import org.jetbrains.plugins.spotbugs.intentions.ClearBugIntentionAction;
import org.jetbrains.plugins.spotbugs.intentions.SuppressReportBugForClassIntentionAction;
import org.jetbrains.plugins.spotbugs.intentions.SuppressReportBugIntentionAction;
import org.jetbrains.plugins.spotbugs.resources.ResourcesLoader;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BugAnnotator implements Annotator {

	public BugAnnotator() {
	}

	@Override
	public void annotate(@NotNull final PsiElement psiElement, @NotNull final AnnotationHolder annotationHolder) {
		final Project project = psiElement.getProject();
		final WorkspaceSettings workspaceSettings = WorkspaceSettings.getInstance(project);
		if (!workspaceSettings.annotationTextRangeMarkup) {
			return;
		}
		if (!FindBugsState.get(project).isIdle()) {
			return;
		}
		final ProblemCacheService cacheService = psiElement.getProject().getService(ProblemCacheService.class);
		if (cacheService == null) {
			return;
		}
		final Map<PsiFile, List<ExtendedProblemDescriptor>> problems = cacheService.getProblems();

		final PsiFile psiFile = psiElement.getContainingFile();
		if (problems.containsKey(psiFile)) {
			addAnnotation(psiElement, new ArrayList<>(problems.get(psiFile)), annotationHolder);
		}
	}

	private static void addAnnotation(@NotNull final PsiElement psiElement, final Iterable<ExtendedProblemDescriptor> problemDescriptors, @NotNull final AnnotationHolder annotationHolder) {
		final List<ExtendedProblemDescriptor> matchingDescriptors = new ArrayList<ExtendedProblemDescriptor>();
		for (final ExtendedProblemDescriptor descriptor : problemDescriptors) {
			final PsiElement problemPsiElement = descriptor.getPsiElement();

			if (psiElement.equals(problemPsiElement)) {
				matchingDescriptors.add(descriptor);
				addAnnotation(descriptor, matchingDescriptors, psiElement, annotationHolder);
			}
		}
	}

	private static void addAnnotation(final ExtendedProblemDescriptor problemDescriptor, final List<ExtendedProblemDescriptor> matchingDescriptors, final PsiElement psiElement, @NotNull final AnnotationHolder annotationHolder) {
		final BugInstance bugInstance = problemDescriptor.getBug().getInstance();
		final int priority = bugInstance.getPriority();
		final Annotation annotation;
		final PsiElement problemElement = problemDescriptor.getPsiElement();
		final TextRange textRange = problemElement.getTextRange();


		switch (priority) {
			case Detector.HIGH_PRIORITY:

				if (psiElement instanceof PsiClass) {
					PsiElement elementToAnnotation;
					if (psiElement instanceof PsiAnonymousClass) {
						elementToAnnotation = psiElement.getFirstChild();
					} else {
						elementToAnnotation = ((PsiClass) psiElement).getNameIdentifier();
					}

					annotation = annotationHolder.createWarningAnnotation(elementToAnnotation == null ? psiElement : elementToAnnotation, getAnnotationText(matchingDescriptors));
					// FIXME: use color from annotation configuration
					annotation.setEnforcedTextAttributes(new TextAttributes(null, null, JBColor.RED.brighter(), EffectType.BOXED, Font.PLAIN));
				} else {
					annotation = annotationHolder.createWarningAnnotation(textRange, getAnnotationText(matchingDescriptors));
					// FIXME: use color from annotation configuration
					annotation.setEnforcedTextAttributes(new TextAttributes(null, null, JBColor.RED, EffectType.WAVE_UNDERSCORE, Font.PLAIN));
				}

				annotation.registerFix(new SuppressReportBugIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new SuppressReportBugForClassIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new ClearBugIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new ClearAndSuppressBugIntentionAction(problemDescriptor), textRange);

				break;

			case Detector.NORMAL_PRIORITY:

				if (psiElement instanceof PsiClass) {
					PsiElement elementToAnnotation;
					if (psiElement instanceof PsiAnonymousClass) {
						elementToAnnotation = psiElement.getFirstChild();
					} else {
						elementToAnnotation = ((PsiClass) psiElement).getNameIdentifier();
					}
					annotation = annotationHolder.createWarningAnnotation(elementToAnnotation == null ? psiElement : elementToAnnotation, getAnnotationText(matchingDescriptors));
				} else {
					annotation = annotationHolder.createWarningAnnotation(textRange, getAnnotationText(matchingDescriptors));
				}

				// FIXME: use color from annotation configuration
				annotation.setEnforcedTextAttributes(new TextAttributes(null, null, JBColor.YELLOW.darker(), EffectType.WAVE_UNDERSCORE, Font.PLAIN));

				annotation.registerFix(new SuppressReportBugIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new SuppressReportBugForClassIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new ClearBugIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new ClearAndSuppressBugIntentionAction(problemDescriptor), textRange);

				break;

			case Detector.EXP_PRIORITY:

				if (problemElement instanceof PsiClass) {
					PsiElement elementToAnnotation;
					if (problemElement instanceof PsiAnonymousClass) {
						elementToAnnotation = problemElement.getFirstChild();
					} else {
						elementToAnnotation = ((PsiClass) problemElement).getNameIdentifier();
					}
					annotation = annotationHolder.createWarningAnnotation(elementToAnnotation == null ? psiElement : elementToAnnotation, getAnnotationText(matchingDescriptors));
				} else {
					annotation = annotationHolder.createWarningAnnotation(textRange, getAnnotationText(matchingDescriptors));
				}

				// FIXME: use color from annotation configuration
				annotation.setEnforcedTextAttributes(new TextAttributes(null, null, JBColor.GRAY, EffectType.WAVE_UNDERSCORE, Font.PLAIN));

				annotation.registerFix(new SuppressReportBugIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new SuppressReportBugForClassIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new ClearBugIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new ClearAndSuppressBugIntentionAction(problemDescriptor), textRange);

				break;
			case Detector.LOW_PRIORITY:

				if (psiElement instanceof PsiClass) {
					PsiElement elementToAnnotation;
					if (psiElement instanceof PsiAnonymousClass) {
						elementToAnnotation = psiElement.getFirstChild();
					} else {
						elementToAnnotation = ((PsiClass) psiElement).getNameIdentifier();
					}
					annotation = annotationHolder.createInfoAnnotation(elementToAnnotation == null ? psiElement : elementToAnnotation, getAnnotationText(matchingDescriptors));
				} else {
					annotation = annotationHolder.createInfoAnnotation(textRange, getAnnotationText(matchingDescriptors));
				}

				// FIXME: use color from annotation configuration
				annotation.setEnforcedTextAttributes(new TextAttributes(null, null, JBColor.GREEN, EffectType.WAVE_UNDERSCORE, Font.PLAIN));

				annotation.registerFix(new SuppressReportBugIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new SuppressReportBugForClassIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new ClearBugIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new ClearAndSuppressBugIntentionAction(problemDescriptor), textRange);

				break;
			case Detector.IGNORE_PRIORITY:
				if (problemElement instanceof PsiClass) {
					PsiElement elementToAnnotation;
					if (problemElement instanceof PsiAnonymousClass) {
						elementToAnnotation = problemElement.getFirstChild();
					} else {
						elementToAnnotation = ((PsiClass) problemElement).getNameIdentifier();
					}
					annotation = annotationHolder.createWarningAnnotation(elementToAnnotation == null ? psiElement : elementToAnnotation, getAnnotationText(matchingDescriptors));
					annotation.setEnforcedTextAttributes(new TextAttributes(null, null, JBColor.MAGENTA.brighter(), EffectType.WAVE_UNDERSCORE, Font.PLAIN));
				} else {
					annotation = annotationHolder.createWarningAnnotation(textRange, getAnnotationText(matchingDescriptors));
				}

				// FIXME: use color from annotation configuration
				annotation.setEnforcedTextAttributes(new TextAttributes(null, null, JBColor.MAGENTA, EffectType.WAVE_UNDERSCORE, Font.PLAIN));

				annotation.registerFix(new SuppressReportBugIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new SuppressReportBugForClassIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new ClearBugIntentionAction(problemDescriptor), textRange);
				annotation.registerFix(new ClearAndSuppressBugIntentionAction(problemDescriptor), textRange);

				break;
			default:
				throw new IllegalArgumentException("Unknown bugInstance.getPriority() == " + priority);
		}
	}

	private static String getAnnotationText(final List<ExtendedProblemDescriptor> problemDescriptors) {
		final StringBuilder buffer = new StringBuilder();
		for (//noinspection LocalCanBeFinal
				int i = 0, problemDescriptorsSize = problemDescriptors.size(); i < problemDescriptorsSize; i++) {
			final ExtendedProblemDescriptor problemDescriptor = problemDescriptors.get(i);
			buffer.append(ResourcesLoader.getString("findbugs.name")).append(": ").append(StringUtilFb.html2text(BugInstanceUtil.getBugPatternShortDescription(problemDescriptor.getBug().getInstance()))).append('\n');
			buffer.append(StringUtilFb.html2text(BugInstanceUtil.getDetailText(problemDescriptor.getBug().getInstance())));
			if (i < problemDescriptors.size() - 1) {
				//noinspection HardcodedLineSeparator
				buffer.append("\n\n");
			}
		}

		return StringUtilFb.addLineSeparatorAt(buffer, 250).toString();
	}

	/*private static class AnonymousInnerClassMayBeStaticVisitor extends BaseInspectionVisitor {

		@Override
		public void visitClass(@NotNull PsiClass aClass) {
			if (!(aClass instanceof PsiAnonymousClass)) {
				return;
			}
			if (aClass instanceof PsiEnumConstantInitializer) {
				return;
			}
			final PsiMember containingMember = PsiTreeUtil.getParentOfType(aClass, PsiMember.class);
			if (containingMember == null || containingMember.hasModifierProperty(PsiModifier.STATIC)) {
				return;
			}
			final PsiAnonymousClass anAnonymousClass = (PsiAnonymousClass) aClass;
			final InnerClassReferenceVisitor visitor = new InnerClassReferenceVisitor(anAnonymousClass);
			anAnonymousClass.accept(visitor);
			if (!visitor.canInnerClassBeStatic()) {
				return;
			}
			registerClassError(aClass);
		}
	}*/
}
