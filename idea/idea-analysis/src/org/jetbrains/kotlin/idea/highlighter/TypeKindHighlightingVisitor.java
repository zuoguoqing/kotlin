/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;

class TypeKindHighlightingVisitor extends AfterAnalysisHighlightingVisitor {
    TypeKindHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext) {
        super(holder, bindingContext);
    }

    @Override
    public void visitNameReferenceExpression(@NotNull KtNameReferenceExpression expression) {
        if (NameHighlighter.INSTANCE.getNamesHighlightingEnabled()) {
            DeclarationDescriptor referenceTarget = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
            if (referenceTarget instanceof ConstructorDescriptor) {
                referenceTarget = referenceTarget.getContainingDeclaration();
            }

            if (referenceTarget instanceof ClassDescriptor) {
                TextAttributesKey textAttributesKey = textAttributesKeyForClass((ClassDescriptor) referenceTarget);
                if (textAttributesKey == KotlinHighlightingColors.ANNOTATION) {
                    highlightAnnotation(expression);
                }
                else {
                    highlightName(expression, textAttributesKey);
                }
            }
            else if (referenceTarget instanceof TypeParameterDescriptor) {
                highlightName(expression, KotlinHighlightingColors.TYPE_PARAMETER);
            }
        }
    }

    private void highlightAnnotation(@NotNull KtSimpleNameExpression expression) {
        TextRange range = expression.getTextRange();

        // include '@' symbol if the reference is the first segment of KtAnnotationEntry
        // if "Deprecated" is highlighted then '@' should be highlighted too in "@Deprecated"
        KtAnnotationEntry annotationEntry = PsiTreeUtil.getParentOfType(
                expression, KtAnnotationEntry.class, /* strict = */false, KtValueArgumentList.class);
        if (annotationEntry != null) {
            PsiElement atSymbol = annotationEntry.getAtSymbol();
            if (atSymbol != null) {
                range = new TextRange(atSymbol.getTextRange().getStartOffset(), expression.getTextRange().getEndOffset());
            }
        }

        NameHighlighter.highlightName(holder, range, KotlinHighlightingColors.ANNOTATION);
    }

    @Override
    public void visitTypeParameter(@NotNull KtTypeParameter parameter) {
        PsiElement identifier = parameter.getNameIdentifier();
        if (identifier != null) {
            highlightName(identifier, KotlinHighlightingColors.TYPE_PARAMETER);
        }
        super.visitTypeParameter(parameter);
    }

    @Override
    public void visitClassOrObject(@NotNull KtClassOrObject classOrObject) {
        PsiElement identifier = classOrObject.getNameIdentifier();
        ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject);
        if (identifier != null && classDescriptor != null) {
            highlightName(identifier, textAttributesKeyForClass(classDescriptor));
        }
        super.visitClassOrObject(classOrObject);
    }

    @Override
    public void visitDynamicType(@NotNull KtDynamicType type) {
        // Do nothing: 'dynamic' is highlighted as a keyword
    }

    private void highlightName(@NotNull PsiElement whatToHighlight, @NotNull TextAttributesKey textAttributesKey) {
        NameHighlighter.highlightName(holder, whatToHighlight, textAttributesKey);
    }

    @NotNull
    private static TextAttributesKey textAttributesKeyForClass(@NotNull ClassDescriptor descriptor) {
        switch (descriptor.getKind()) {
            case INTERFACE:
                return KotlinHighlightingColors.TRAIT;
            case ANNOTATION_CLASS:
                return KotlinHighlightingColors.ANNOTATION;
            case OBJECT:
                return KotlinHighlightingColors.OBJECT;
            case ENUM_ENTRY:
                return KotlinHighlightingColors.ENUM_ENTRY;
            default:
                return descriptor.getModality() == Modality.ABSTRACT
                       ? KotlinHighlightingColors.ABSTRACT_CLASS
                       : KotlinHighlightingColors.CLASS;
        }
    }
}
