package com.whiker.learn.common.dynamiccompile;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import java.util.Locale;

/**
 * @author whiker@163.com create on 16-5-7.
 */
public class DiagnosticUtil {

    public static String toString(DiagnosticCollector collector) {
        StringBuilder str = new StringBuilder();

        for (Object object : collector.getDiagnostics()) {
            Diagnostic diagnostic = (Diagnostic) object;
            str.append(String.format("\n%s:Line %d, %s", diagnostic.getKind().toString(), diagnostic.getLineNumber(),
                    diagnostic.getMessage(Locale.ENGLISH)));
        }
        return str.toString();
    }
}
