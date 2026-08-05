package dev.lukebemish.opensesame.compile.javac;

import com.google.auto.service.AutoService;
import dev.lukebemish.opensesame.compile.asm.VisitingProcessor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Set;

@SupportedAnnotationTypes({
        "dev.lukebemish.opensesame.annotations.mixin.*"
})
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class GradleIncrementalServicesAdapter extends AbstractProcessor {
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            try {
                VisitingProcessor.getMixinServiceLocation(it -> Paths.get(processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", it).toUri()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return false;
    }
}
