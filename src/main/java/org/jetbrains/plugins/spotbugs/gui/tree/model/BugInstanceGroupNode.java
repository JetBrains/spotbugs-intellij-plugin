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
package org.jetbrains.plugins.spotbugs.gui.tree.model;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import edu.umd.cs.findbugs.BugRankCategory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.spotbugs.core.Bug;
import org.jetbrains.plugins.spotbugs.gui.tree.BugInstanceComparator;
import org.jetbrains.plugins.spotbugs.gui.tree.GroupBy;
import org.jetbrains.plugins.spotbugs.gui.tree.NodeVisitor;
import org.jetbrains.plugins.spotbugs.gui.tree.RecurseNodeVisitor;
import org.jetbrains.plugins.spotbugs.gui.tree.RecurseNodeVisitor.RecurseVisitCriteria;
import org.jetbrains.plugins.spotbugs.gui.tree.view.MaskIcon;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class BugInstanceGroupNode extends AbstractTreeNode<VisitableTreeNode> implements VisitableTreeNode {

	private final List<VisitableTreeNode> _children;
	private final Bug bug;
	private final RecurseNodeVisitor<BugInstanceGroupNode> _recurseNodeVisitor = new RecurseNodeVisitor<>(this);
	private final Project _project;

	/**
	 * Creates a new instance of BugInstanceGroup.
	 *
	 * @param groupBy   string indicating why the bug instances in the group
	 *                  are related
	 * @param groupName name of the group (e.g., the class name if the group
	 * @param parent    the parent tree node
	 * @param bug       the bug instance
	 * @param depth     the category depth from root category
	 * @param project   the idea project
	 */
	BugInstanceGroupNode(final GroupBy groupBy, final String groupName, final VisitableTreeNode parent, final Bug bug, final int depth, final Project project) {
		setParent(parent);
		_project = project;
		this.bug = bug;
		_children = new ArrayList<>();
		_groupBy = groupBy;
		_groupName = groupName;
		_simpleName = groupName;
		_memberCount = 0;
		_depthFromRoot = depth;

		setTooltip(_groupName);
		setCollapsedIcon(new MaskIcon(getGroupByCollapsedIcon(groupBy), JBColor.BLACK));
		setExpandedIcon(new MaskIcon(getGroupByExpandedIcon(groupBy), JBColor.BLACK));
	}

	Project getProject() {
		return _project;
	}

	@Override
	public void addChild(final VisitableTreeNode node) {
		_children.add(node);
		if (node instanceof BugInstanceNode && node.isLeaf()) {
			_children.sort(new ChildComparator());
			incrementMemberCount();
			TreeNode treeNode = getParent();
			while (treeNode instanceof BugInstanceGroupNode) {
				((BugInstanceGroupNode) treeNode).incrementMemberCount();
				treeNode = treeNode.getParent();
			}
		}
	}

	@Nullable
	public BugInstanceGroupNode findChildNode(final Bug bug, final int depth, final String groupName) {
		if (Bug.equalsBugType(this.bug, bug) && Bug.equalsBugClass(this.bug, bug) && depth == _depthFromRoot && groupName.equals(_groupName)) {
			return this;
		}
		final RecurseVisitCriteria criteria = new RecurseVisitCriteria(bug, depth, groupName);
		return _recurseNodeVisitor.findChildNode(criteria);
	}

	@Override
	public void accept(final NodeVisitor visitor) {
		visitor.visitGroupNode(this);
	}

	@NotNull
	List<Bug> getAllChildBugs() {
    final List<Bug> ret = new ArrayList<>();
		for (final TreeNode child : _children) {
			if (child instanceof BugInstanceGroupNode) {
				final BugInstanceGroupNode node = (BugInstanceGroupNode) child;
				final List<Bug> bugs = node.getAllChildBugs();
				ret.addAll(ret.size(), bugs);
			}
		}
		return ret;
	}

	public static TreePath getPath(TreeNode node) {
    final List<TreeNode> list = new ArrayList<>();
		while (node != null) {
			list.add(node);
			//noinspection AssignmentToMethodParameter
			node = node.getParent();
		}
		Collections.reverse(list);
		return new TreePath(list.toArray());
	}

	public String[] getPath() {
		final List<String> path = _getPath();
		return getPath(path.size());
	}

	private String[] getPath(final int depth) {
		final List<String> path = _getPath();
		final String[] result = new String[depth];
		for (int i = 0; i < depth; i++) {
			final String str = path.get(i);
			result[i] = str;
		}
		return result;
	}

	private List<String> _getPath() {
    final List<String> list = new ArrayList<>();
		TreeNode node = this;
		while (node != null) {
			if (node instanceof BugInstanceGroupNode) {
				list.add(((BugInstanceGroupNode) node).getGroupName());
				node = node.getParent();
			}
		}
		return list;
	}

	@Override
	public List<VisitableTreeNode> getChildsList() {
		return _children;
	}

	@Override
	public BugInstanceGroupNode getTreeNode() {
		return this;
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public boolean isLeaf() {
		return _children.isEmpty();
	}

	private void incrementMemberCount() {
		++_memberCount;
	}

	public Bug getBug() {
		return bug;
	}

	private Icon getGroupByCollapsedIcon(final GroupBy groupBy) {
		switch (groupBy) {
			case BugCategory:
				return PluginIcons.GROUP_BY_CATEGORY_ICON;
			case Class:
				return PluginIcons.GROUP_BY_CLASS_ICON;
			case Package:
				return PluginIcons.GROUP_BY_PACKAGE_ICON;
			case Priority:
				final String priorityString = bug.getInstance().getPriorityString();
				return PluginIcons.GROUP_BY_PRIORITY_ICONS.getOrDefault(priorityString, PluginIcons.GROUP_BY_PRIORITY_EXP_ICON);
			case BugRank:
				final String rankString = BugRankCategory.getRank(bug.getInstance().getBugRank()).toString().toUpperCase(Locale.ENGLISH);
				return PluginIcons.GROUP_BY_RANK_ICONS.getOrDefault(rankString, PluginIcons.GROUP_BY_PRIORITY_ICON);
			default:
				return _collapsedIcon;
		}
	}

	private Icon getGroupByExpandedIcon(final GroupBy groupBy) {
		switch (groupBy) {
			case BugCategory:
				return PluginIcons.GROUP_BY_CATEGORY_ICON;
			case Class:
				return PluginIcons.GROUP_BY_CLASS_ICON;
			case Package:
				return PluginIcons.GROUP_BY_PACKAGE_ICON;
			case Priority:
				final String priorityString = bug.getInstance().getPriorityString();
				return PluginIcons.GROUP_BY_PRIORITY_ICONS.getOrDefault(priorityString, PluginIcons.GROUP_BY_PRIORITY_EXP_ICON);
			case BugRank:
				final String rankString = BugRankCategory.getRank(bug.getInstance().getBugRank()).name();
				return PluginIcons.GROUP_BY_RANK_ICONS.getOrDefault(rankString, PluginIcons.GROUP_BY_PRIORITY_ICON);
			default:
				return _expandedIcon;
		}
	}

	@Override
	public String toString() {
		return "BugInstanceGroupNode" +
					 "{_childs=" + _children +
					 ", bug=" + bug +
					 ", _recurseNodeVisitor=" + _recurseNodeVisitor +
					 '}';
	}

	private static class ChildComparator implements Comparator<TreeNode> {
		@SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST")
		public int compare(final TreeNode a, final TreeNode b) {
			if (!(a instanceof BugInstanceNode) || !(b instanceof BugInstanceNode)) {
				throw new IllegalArgumentException("argument not instance of BugInstanceNode.");
			}
			return BugInstanceComparator.getBugInstanceClassComparator().compare(((BugInstanceNode) a).getBug(), ((BugInstanceNode) b).getBug());
		}
	}
}
