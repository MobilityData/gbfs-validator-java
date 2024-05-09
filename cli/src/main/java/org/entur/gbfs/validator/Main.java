package org.entur.gbfs.validator;

import org.entur.gbfs.validation.GbfsValidatorFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class Main
{
    public static void main( String[] args )
    {
        if (args.length < 1) {
            printHelp();
        } else {
            Map<String, String> argsMap = new HashMap<>();
            Arrays.stream(args).forEach(arg -> {
                var splitArg = arg.split("=");
                if (splitArg[0].equals("-u") || splitArg[0].equals("--url")) {
                    argsMap.put("url", splitArg[1]);
                }
            });

            if (!argsMap.containsKey("url")) {
                printHelp();
            }

            Loader loader = new Loader();
            var map = loader.load(argsMap.get("url"));
            var validator = GbfsValidatorFactory.getGbfsJsonValidator();
            long start = System.currentTimeMillis();
            var result = validator.validate(map);
            long finish = System.currentTimeMillis();
            System.out.println(result.summary());

            long timeElapsed = finish - start;
            System.out.println("Execution time validation: " + timeElapsed + "ms");
        }
    }

    private static void printHelp() {
        System.out.println(
            """
                -v,  --version: output the version number
                -u,  --url <feed_url>: URL of the GBFS feed
                -vb, --verbose: Verbose mode prints debugging console logs
                -s,  --save-report <report_path>: Local path to output report file
                -pr, --print-report <yes_no>: Print report to standard output (choices: "yes", "no", default: "yes")
                -h,  --help: display help for command
            """.stripIndent());
    }
}
