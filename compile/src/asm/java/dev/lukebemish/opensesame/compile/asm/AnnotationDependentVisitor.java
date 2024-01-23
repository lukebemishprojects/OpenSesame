package dev.lukebemish.opensesame.compile.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;
import java.util.List;

public class AnnotationDependentVisitor extends ClassVisitor {
    protected AnnotationDependentVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    public static class ClassInfo {
        int version;
        int access;
        String name;
        String signature;
        String superName;
        String[] interfaces;
    }
    ClassInfo classInfo;

    public static class SourceInfo {
        String source;
        String debug;
    }
    SourceInfo sourceInfo;
    ModuleNode moduleNode;

    public static class NestInfo {
        String nestHost;
    }
    NestInfo nestInfo;

    public static class OuterInfo {
        String outerOwner;
        String outerName;
        String outerDescriptor;
    }
    OuterInfo outerInfo;

    public sealed interface AnnotationOrAttribute {}
    public record AnnotationInfo(AnnotationNode node, boolean visible) implements AnnotationOrAttribute {}
    public record TypeAnnotationInfo(TypeAnnotationNode node, boolean visible) implements AnnotationOrAttribute {}
    public record AttributeInfo(Attribute attribute) implements AnnotationOrAttribute {}
    List<AnnotationOrAttribute> annotationsAndAttributes = new ArrayList<>();

    boolean dumped = false;
    private synchronized void dump() {
        if (dumped) return;
        dumped = true;
        super.visit(classInfo.version, classInfo.access, classInfo.name, classInfo.signature, classInfo.superName, classInfo.interfaces);
        if (sourceInfo != null) super.visitSource(sourceInfo.source, sourceInfo.debug);
        if (moduleNode != null) {
            moduleNode.accept(getDelegate());
        }
        if (nestInfo != null) super.visitNestHost(nestInfo.nestHost);
        if (outerInfo != null) super.visitOuterClass(outerInfo.outerOwner, outerInfo.outerName, outerInfo.outerDescriptor);
        for (var stuff : annotationsAndAttributes) {
            if (stuff instanceof AnnotationInfo annotationInfo) {
                annotationInfo.node.accept(getDelegate().visitAnnotation(annotationInfo.node.desc, annotationInfo.visible));
            } else if (stuff instanceof TypeAnnotationInfo typeAnnotationInfo) {
                typeAnnotationInfo.node.accept(getDelegate().visitTypeAnnotation(typeAnnotationInfo.node.typeRef, typeAnnotationInfo.node.typePath, typeAnnotationInfo.node.desc, typeAnnotationInfo.visible));
            } else if (stuff instanceof AttributeInfo attributeInfo) {
                super.visitAttribute(attributeInfo.attribute);
            }
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        classInfo = new ClassInfo();
        classInfo.version = version;
        classInfo.access = access;
        classInfo.name = name;
        classInfo.signature = signature;
        classInfo.superName = superName;
        classInfo.interfaces = interfaces;
    }

    @Override
    public void visitSource(String source, String debug) {
        sourceInfo = new SourceInfo();
        sourceInfo.source = source;
        sourceInfo.debug = debug;
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {
        ModuleNode module = new ModuleNode(name, access, version);
        moduleNode = module;
        return module;
    }

    @Override
    public void visitNestHost(String nestHost) {
        nestInfo = new NestInfo();
        nestInfo.nestHost = nestHost;
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        outerInfo = new OuterInfo();
        outerInfo.outerOwner = owner;
        outerInfo.outerName = name;
        outerInfo.outerDescriptor = descriptor;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationNode annotationNode = new AnnotationNode(descriptor);
        annotationsAndAttributes.add(new AnnotationInfo(annotationNode, visible));
        return annotationNode;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        TypeAnnotationNode typeAnnotationNode = new TypeAnnotationNode(typeRef, typePath, descriptor);
        annotationsAndAttributes.add(new TypeAnnotationInfo(typeAnnotationNode, visible));
        return typeAnnotationNode;
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        annotationsAndAttributes.add(new AttributeInfo(attribute));
    }

    @Override
    public void visitNestMember(String nestMember) {
        dump();
        super.visitNestMember(nestMember);
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {
        dump();
        super.visitPermittedSubclass(permittedSubclass);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        dump();
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        dump();
        return super.visitRecordComponent(name, descriptor, signature);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        dump();
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        dump();
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        dump();
        super.visitEnd();
    }
}
