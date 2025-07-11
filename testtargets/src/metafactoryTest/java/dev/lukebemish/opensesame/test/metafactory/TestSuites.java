package dev.lukebemish.opensesame.test.metafactory;


import org.junit.jupiter.api.DisplayName;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectModules;
import org.junit.platform.suite.api.Suite;

@Suite
@DisplayName("OpenSesame Core Suite")
@SelectModules("dev.lukebemish.opensesame.test.metafactory")
@IncludeClassNamePatterns(".*\\.Test.*")
@IncludeEngines({"junit-jupiter", "opensesame-delegate"})
public class TestSuites {
    
}
