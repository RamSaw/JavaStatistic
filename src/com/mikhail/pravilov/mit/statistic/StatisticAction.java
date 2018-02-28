package com.mikhail.pravilov.mit.statistic;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import javafx.util.Pair;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static com.mikhail.pravilov.mit.statistic.StatisticAction.StatisticType.*;

/**
 * Class that describes action in Intelij Platform, gathers statistic information about java classes and creates a window.
 */
public class StatisticAction extends AnAction {
    /**
     * Constructs {@link StatisticAction} instance with text and description, without icon.
     */
    public StatisticAction() {
        super("Statistics", "Program that gathers project statistic", null);
    }

    /**
     * Constructs {@link StatisticAction} instance with passed parameters.
     * @param text of action.
     * @param description of action.
     * @param icon of action.
     */
    public StatisticAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Pair<EnumMap<StatisticType, Integer>, HashMap<PsiFile, EnumMap<StatisticType, Integer>>> statistic;
        statistic = getStatistic(project);

        Messages.showMessageDialog(project, getStatisticInString(statistic),
                "Statistic", null);
    }

    /**
     * Gathers statistic described by {@link StatisticType}.
     * @param project where to gather statistic.
     * @return pair: the first element is project statistic, the second one is statistic specific for a class.
     */
    private Pair<EnumMap<StatisticType, Integer>, HashMap<PsiFile, EnumMap<StatisticType, Integer>>> getStatistic(Project project) {
        EnumMap<StatisticType, Integer> projectStatistic = getNewStatisticEnumMapInstance();
        HashMap<PsiFile, EnumMap<StatisticType, Integer>> classStatistic = new HashMap<>();

        FileTypeIndex.processFiles(JavaFileType.INSTANCE, virtualFile -> {
            addStatistic(projectStatistic, NUMBER_OF_CLASSES, 1);

            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

            if (psiFile != null) {
                if (!classStatistic.containsKey(psiFile)) {
                    classStatistic.put(psiFile, getNewStatisticEnumMapInstance());
                }

                psiFile.accept(new JavaRecursiveElementVisitor() {
                    @Override
                    public void visitMethod(PsiMethod method) {
                        super.visitMethod(method);

                        addStatistic(projectStatistic, NUMBER_OF_METHODS, 1);
                        addStatistic(projectStatistic, LENGTH_OF_METHODS, method.getTextLength());
                        addStatistic(classStatistic.get(psiFile), NUMBER_OF_METHODS, 1);
                        addStatistic(classStatistic.get(psiFile), LENGTH_OF_METHODS, method.getTextLength());
                    }

                    @Override
                    public void visitField(PsiField field) {
                        super.visitField(field);

                        addStatistic(projectStatistic, NUMBER_OF_FIELDS, 1);
                        addStatistic(classStatistic.get(psiFile), NUMBER_OF_FIELDS, 1);
                        addStatistic(classStatistic.get(psiFile), LENGTH_OF_FIELDS_NAMES, field.getNameIdentifier().getTextLength());
                    }
                });

                return true;
            }

            return false;
        }, GlobalSearchScope.projectScope(project));

        return new Pair<>(projectStatistic, classStatistic);
    }

    /**
     * Prepares statistic for user by converting it to string.
     * @param statistic that action has gathered.
     * @return result string to be printed to user.
     */
    private String getStatisticInString(Pair<EnumMap<StatisticType, Integer>, HashMap<PsiFile, EnumMap<StatisticType, Integer>>> statistic) {
        EnumMap<StatisticType, Integer> projectStatistic = statistic.getKey();

        StringBuilder statisticInString = new StringBuilder("Total number of classes (*.java files): " +
                projectStatistic.get(NUMBER_OF_CLASSES) + "\n");
        statisticInString.append("Total number of methods: ").append(projectStatistic.get(NUMBER_OF_METHODS)).append("\n");
        statisticInString.append("Average length of methods in project: ");
        if (projectStatistic.get(NUMBER_OF_METHODS) != 0) {
            statisticInString.append((double) projectStatistic.get(LENGTH_OF_METHODS) / projectStatistic.get(NUMBER_OF_METHODS));
        }
        else {
            statisticInString.append("No methods");
        }
        statisticInString.append("\n");
        statisticInString.append("Average number of fields in class: ");
        if (projectStatistic.get(NUMBER_OF_CLASSES) != 0) {
            statisticInString.append((double) projectStatistic.get(NUMBER_OF_FIELDS) / projectStatistic.get(NUMBER_OF_CLASSES));
        }
        else {
            statisticInString.append("No classes");
        }
        statisticInString.append("\n");
        statisticInString.append("------------\n");

        for (Map.Entry<PsiFile, EnumMap<StatisticType, Integer>> classStatisticPair : statistic.getValue().entrySet()) {
            PsiFile currentClass = classStatisticPair.getKey();
            EnumMap<StatisticType, Integer> classStatistic = classStatisticPair.getValue();

            statisticInString.append("Statistic for class ").append(currentClass.getName()).append("\n");
            statisticInString.append("Average length of methods in class: ");
            if (classStatistic.get(NUMBER_OF_METHODS) != 0) {
                statisticInString.append((double) classStatistic.get(LENGTH_OF_METHODS) / classStatistic.get(NUMBER_OF_METHODS));
            }
            else {
                statisticInString.append("No methods");
            }
            statisticInString.append("\n");
            statisticInString.append("Average length of field names in class: ");
            if (classStatistic.get(NUMBER_OF_FIELDS) != 0) {
                statisticInString.append((double) classStatistic.get(LENGTH_OF_FIELDS_NAMES) / classStatistic.get(NUMBER_OF_FIELDS));
            }
            else {
                statisticInString.append("No fields");
            }
            statisticInString.append("\n");
            statisticInString.append("------------\n");
        }

        return statisticInString.toString();
    }

    /**
     * Method to add (sum of integers) new value to the specific type of statistic.
     * @param statistic to update.
     * @param statisticType type of statistic described by {@link StatisticType}.
     * @param add number to add.
     */
    private void addStatistic(EnumMap<StatisticType, Integer> statistic, StatisticType statisticType, Integer add) {
        statistic.put(statisticType, statistic.get(statisticType) + add);
    }

    /**
     * Creates new instance of EnumMap to store statistic. All keys are described by {@link StatisticType} and are 0 by default.
     * @return created EnumMap instance.
     */
    private EnumMap<StatisticType,Integer> getNewStatisticEnumMapInstance() {
        return new EnumMap<StatisticType, Integer>(StatisticType.class) {{
            put(NUMBER_OF_CLASSES, 0);
            put(NUMBER_OF_METHODS, 0);
            put(LENGTH_OF_METHODS, 0);
            put(NUMBER_OF_FIELDS, 0);
            put(LENGTH_OF_FIELDS_NAMES, 0);
        }};
    }

    /**
     * Enum that describes all types of statistic that are supported by this action.
     */
    public enum StatisticType {
        NUMBER_OF_CLASSES, NUMBER_OF_METHODS, LENGTH_OF_METHODS, NUMBER_OF_FIELDS, LENGTH_OF_FIELDS_NAMES
    }
}
