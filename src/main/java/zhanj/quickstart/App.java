package zhanj.quickstart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class App {
    /**
     * java Quickstart --group-id=zhanj --artifact-id=template --version=1.0-SNAPSHOT --spring-boot-version=2.0.5
     * @param args
     */
    public static void main( String[] args) {
        Options options = new Options();
        options.addRequiredOption("g", "group-id", true, "group name of this project");
        options.addRequiredOption("a", "artifact-id", true, "name of this project");
        options.addOption("v", "version", true, "version of this project");
        options.addOption("m", "main-class", true, "main class of this project");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);
            String groupId = line.getOptionValue("g");
            String artifactId = line.getOptionValue("a");
            String version = line.getOptionValue("v", "1.0-SNAPSHOT");
            String mainClass = line.getOptionValue("m", "App");

            String error;
            if ((error = checkParams(artifactId, groupId, mainClass, version)) != null) {
                System.err.println("[ERROR] " + error);
                return;
            }

            String packageName = groupId + "." + artifactId.replace("-", "");

            // make directory
            String sourceDir = String.format("%s/src/main/java/%s", artifactId, packageName.replace('.', '/'));
            String resourcesDir = artifactId + "/src/main/resources";
            createDirectory(sourceDir);
            createDirectory(resourcesDir);

            Map<String, String> model = new HashMap<>();
            model.put("group_id", groupId);
            model.put("artifact_id", artifactId);
            model.put("version", version);
            model.put("package_name", packageName);
            model.put("main_class", mainClass);

            // generate pom.xml
            String pom = readResourceFile("pom_xml");
            List<Param> params = findParams(pom);
            String result = generate(pom, params, model);
            writeToFile(result, artifactId + "/pom.xml");

            // generate App.java
            String app = readResourceFile("App_java");
            params = findParams(app);
            result = generate(app, params, model);
            writeToFile(result, sourceDir + "/" + mainClass + ".java");

            System.out.printf("project '%s' is generated in current directory.%n", artifactId);
            System.out.println("run command 'mvn package' to generate executable jar archive with dependencies.");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Quickstart", options);
        }
    }

    private static String checkParams(String artifactId, String groupId, String mainClass, String version) {
        if (artifactId.length() > 20 || !artifactId.matches("[a-zA-Z0-9\\-]+")) {
            return "'artifactId' consists of characters from 'a-z A-Z -' and can not be more than 20 characters";
        }
        if (groupId.length() > 32 || !groupId.matches("[a-zA-Z0-9\\.]+")) {
            return "'groupId' consists of characters from 'a-z A-Z .' and can not be more than 32 characters";
        }
        if (mainClass.length() > 15 || !mainClass.matches("[A-Z]+[a-zA-Z0-9]*")) {
            return "'mainClass' consists of characters from 'a-z A-Z 0-9', starts with a capital character and can not be more than 15 characters";
        }
        if (version.length() > 15) {
            return "'version' can not be more than 15 characters";
        }
        return null;
    }

    private static String readResourceFile(String path) throws IOException {
        InputStream stream = ClassLoader.getSystemResourceAsStream(path);
        StringBuilder result = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    private static void createDirectory(String dir) throws Exception {
        File file = new File(dir);
        if (file.exists()) {
            throw new Exception(String.format("file or directory with the name '%s' already exists", dir));
        }
        if (file.isFile()) {
            throw new Exception(String.format("failed to create directory, because a file with the name '%s' already exists", dir));
        }
        if (!file.mkdirs()) {
            throw new Exception("failed to create directory " + dir);
        }
    }

    private static void writeToFile(String content, String filePath) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        writer.write(content);
        writer.flush();
        writer.close();
    }

    private static String generate(String template, List<Param> params, Map<String,String> model) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (Param param : params) {
            if (param.getIndex() > i) {
                result.append(template.substring(i, param.getIndex()));
                i = param.getIndex();
            }
            String value = model.get(param.getName().trim());
            if (value != null) {
                result.append(value);
                i += param.getLen();
            }
        }
        if (i < template.length()) {
            result.append(template.substring(i));
        }
        return result.toString();
    }

    private static List<Param> findParams(String template) {
        List<Param> params = new ArrayList<>();
        int i = 0;
        while (i < template.length()-1) {
            char c = template.charAt(i);
            if (c == '{') {
                char next = template.charAt(i+1);
                if (next == '{') {
                    int len = findParamLength(template, i+2);
                    if (len < 0) {
                        return params;
                    }
                    Param p = new Param();
                    p.setIndex(i);
                    p.setLen(len+4);
                    p.setName(template.substring(i+2, i+2+len));
                    params.add(p);
                    i += p.getLen();
                } else {
                    i += 2;
                }
            } else {
                ++i;
            }
        }
        return params;
    }

    private static int findParamLength(String template, int startIndex) {
        int i = startIndex;
        while (i < template.length()-1) {
            char c = template.charAt(i);
            if (c == '}') {
                char next = template.charAt(i+1);
                if (next == '}') {
                    return i - startIndex;
                } else {
                    i += 2;
                }
            } else {
                i++;
            }
        }
        return -1;
    }
}