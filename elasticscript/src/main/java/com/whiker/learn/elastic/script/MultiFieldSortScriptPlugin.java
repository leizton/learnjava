package com.whiker.learn.elastic.script;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author whiker@163.com create on 16-11-20.
 */
public class MultiFieldSortScriptPlugin extends Plugin implements ScriptPlugin {

    public List<NativeScriptFactory> getNativeScripts() {
        return Collections.singletonList(new MultiFieldSortScriptFactory());
    }

    public static class MultiFieldSortScriptFactory implements NativeScriptFactory {

        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            return new MultiFieldSortScriptImpl(params);
        }

        @Override
        public boolean needsScores() {
            return false;
        }

        @Override
        public String getName() {
            return "multi_fields_sort";
        }
    }

    public static class MultiFieldSortScriptImpl extends AbstractSearchScript {

        private String[] sortByFields;

        public MultiFieldSortScriptImpl(Map<String, Object> params) {
            sortByFields = new String[0];
            if (params != null) {
                Object paramValue = params.get("fields");
                if (paramValue != null && paramValue instanceof String) {
                    sortByFields = ((String) paramValue).split(",");
                }
            }
        }

        @Override
        public Object run() {
            if (sortByFields.length == 0) {
                return "";
            }
            StringBuilder str = new StringBuilder();
            for (String field : sortByFields) {
                str.append(source().get(field));
            }
            return str.toString();
        }
    }
}
