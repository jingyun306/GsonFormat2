package org.gsonformat.intellij.process;

import com.intellij.psi.*;
import org.apache.commons.lang.StringUtils;
import org.apache.http.util.TextUtils;
import org.gsonformat.intellij.common.FieldHelper;
import org.gsonformat.intellij.common.Try;
import org.gsonformat.intellij.config.Config;
import org.gsonformat.intellij.config.Constant;
import org.gsonformat.intellij.entity.ClassEntity;
import org.gsonformat.intellij.entity.FieldEntity;

import java.util.regex.Pattern;

/**
 * Created by dim on 16/11/7.
 */
public class LombokProcessor extends Processor {

    @Override
    protected void onStarProcess(ClassEntity classEntity, PsiElementFactory factory, PsiClass cls,
                                 IProcessor visitor) {
        super.onStarProcess(classEntity, factory, cls, visitor);
        injectAnnotation(factory, cls);
    }

    private void injectAnnotation(PsiElementFactory factory, PsiClass generateClass) {
        if (factory == null || generateClass == null) {
            return;
        }
        PsiModifierList modifierList = generateClass.getModifierList();
        if (modifierList != null) {
            PsiElement firstChild = modifierList.getFirstChild();
            Pattern pattern = Pattern.compile("@.*?ApiModel");
            if (firstChild != null && !pattern.matcher(firstChild.getText()).find()) {
                PsiAnnotation annotationFromText =
                  factory
                    .createAnnotationFromText("@io.swagger.annotations.ApiModel", generateClass);
                modifierList.addBefore(annotationFromText, firstChild);
            }
            Pattern pattern2 = Pattern.compile("@.*?Data");
            if (firstChild != null && !pattern2.matcher(firstChild.getText()).find()) {
                PsiAnnotation annotationFromText =
                  factory.createAnnotationFromText("@lombok.Data", generateClass);
                modifierList.addBefore(annotationFromText, firstChild);
            }
        }
    }

    private void injectFieldAnnotation(PsiElementFactory factory, PsiClass generateClass,
                                       ClassEntity classEntity,
                                       FieldEntity fieldEntity) {
        if (factory == null || generateClass == null) {
            return;
        }
        PsiField[] fields = generateClass.getFields();
        PsiModifierList modifierList = fields[fields.length - 1].getModifierList();
        if (modifierList != null) {
            PsiElement firstChild = modifierList.getFirstChild();
            //Pattern pattern = Pattern.compile("@.*?NoArgsConstructor");
            //if (firstChild != null && !pattern.matcher(firstChild.getText()).find()) {
            //    PsiAnnotation annotationFromText =
            //      factory.createAnnotationFromText("@lombok.NoArgsConstructor", generateClass);
            //    modifierList.addBefore(annotationFromText, firstChild);
            //}
            Pattern pattern2 = Pattern.compile("@.*?ApiModelProperty");
            if (firstChild != null && !pattern2.matcher(firstChild.getText()).find()) {
                PsiAnnotation annotationFromText = null;
                String annotationText = "";
                if (StringUtils.isNotBlank(fieldEntity.getDesc())) {
                    annotationText += "value=\"" + fieldEntity.getDesc()
                                      + "\"";
                }
                if (StringUtils.isNotBlank(fieldEntity.getValue())) {
                    if (StringUtils.isNotBlank(annotationText)) {
                        annotationText += ",example =\"" + fieldEntity.getValue() + "\" ";
                    } else {
                        annotationText += "example =\"" + fieldEntity.getValue() + "\" ";
                    }
                }
                if (StringUtils.isNotBlank(annotationText)) {
                    annotationText = "(" + annotationText + ")";
                }

                annotationFromText =
                  factory.createAnnotationFromText(
                    "@io.swagger.annotations.ApiModelProperty" + annotationText,
                    generateClass);

                modifierList.addBefore(annotationFromText, firstChild);
            }
        }
    }


    @Override
    protected void generateField(PsiElementFactory factory, FieldEntity fieldEntity, PsiClass cls,
                                 ClassEntity classEntity) {
        if (fieldEntity.isGenerate()) {
            Try.run(new Try.TryListener() {
                @Override
                public void run() {
                    cls.add(factory.createFieldFromText(
                      generateLombokFieldText(classEntity, fieldEntity, null), cls));
                }

                @Override
                public void runAgain() {
                    fieldEntity
                      .setFieldName(FieldHelper.generateLuckyFieldName(fieldEntity.getFieldName()));
                    cls.add(factory.createFieldFromText(
                      generateLombokFieldText(classEntity, fieldEntity, Constant.FIXME), cls));
                }

                @Override
                public void error() {
                    cls.addBefore(factory.createCommentFromText(
                      "// FIXME generate failure  field " + fieldEntity.getFieldName(), cls),
                                  cls.getChildren()[0]);
                }
            });
            injectFieldAnnotation(factory, cls, classEntity, fieldEntity);
        }
    }

    @Override
    protected void createGetAndSetMethod(PsiElementFactory factory, PsiClass cls,
                                         FieldEntity field) {
    }

    @Override
    protected void onEndGenerateClass(PsiElementFactory factory, ClassEntity classEntity,
                                      PsiClass parentClass, PsiClass generateClass,
                                      IProcessor visitor) {
        super.onEndGenerateClass(factory, classEntity, parentClass, generateClass, visitor);
        injectAnnotation(factory, generateClass);
    }


    private String generateLombokFieldText(ClassEntity classEntity, FieldEntity fieldEntity,
                                           String fixme) {
        fixme = fixme == null ? "" : fixme;

        StringBuilder fieldSb = new StringBuilder();
        String filedName = fieldEntity.getGenerateFieldName();
        if (!TextUtils.isEmpty(classEntity.getExtra())) {
            fieldSb.append(classEntity.getExtra()).append("\n");
            classEntity.setExtra(null);
        }
        if (fieldEntity.getTargetClass() != null) {
            fieldEntity.getTargetClass().setGenerate(true);
        }

        if (Config.getInstant().isFieldPrivateMode()) {
            fieldSb.append("private  ").append(fieldEntity.getFullNameType()).append(" ")
                   .append(filedName).append(" ; ");
        } else {
            fieldSb.append("public  ").append(fieldEntity.getFullNameType()).append(" ")
                   .append(filedName).append(" ; ");
        }
        return fieldSb.append(fixme).toString();
    }
}
