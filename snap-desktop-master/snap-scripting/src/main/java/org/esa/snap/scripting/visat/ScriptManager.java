/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.scripting.visat;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes all {@link ScriptEngine} code in a single, dedicated thread.
 */
public class ScriptManager {
    private final ClassLoader classLoader;
    private final PrintWriter output;
    private ScriptEngineManager scriptEngineManager;
    private ScriptEngine engine;
    /*
     * executorService has two roles:
     * (1) provide a single thread for all script engine calls
     * (2) create a thread with a dedicated context class loader
     */
    private ExecutorService executorService;

    public ScriptManager(ClassLoader classLoader, PrintWriter output) {
        this.classLoader = classLoader;
        this.output = output;
        executorService = createExecutorService();
        executorService.submit(() -> scriptEngineManager = new ScriptEngineManager(ScriptManager.this.classLoader));
    }

    public ScriptEngineFactory[] getEngineFactories() {
        List<ScriptEngineFactory> scriptEngineFactoryList = scriptEngineManager.getEngineFactories();
        return scriptEngineFactoryList.toArray(new ScriptEngineFactory[scriptEngineFactoryList.size()]);
    }

    public ScriptEngine getEngine() {
        return engine;
    }

    public void setEngine(final ScriptEngine engine) {
        if (this.engine != null && this.engine.getFactory() == engine.getFactory()) {
            return;
        }

        executorService.submit(() -> {
            ScriptManager.this.engine = engine;
            configureEngine();
        });
    }

    public ScriptEngine getEngineByFactory(final ScriptEngineFactory scriptEngineFactory) {

        return getEngine(scriptEngineFactory::getScriptEngine);
    }

    public ScriptEngine getEngineByMimeType(final String mimeType) {
        return getEngine(() -> scriptEngineManager.getEngineByMimeType(mimeType));
    }

    public ScriptEngine getEngineByExtension(final String extension) {
        return getEngine(() -> scriptEngineManager.getEngineByExtension(extension));
    }

    public void execute(final String code, final Observer observer) {

        executorService.submit(() -> execute0(code, observer));
    }


    public void execute(final URL url, final Observer observer) {

        executorService.submit(() -> execute0(url, observer));
    }

    private void execute0(String code, Observer observer) {
        checkEngineSet();
        Object object;
        try {
            object = engine.eval(code);
        } catch (Throwable throwable) {
            observer.onFailure(throwable);
            return;
        }
        observer.onSuccess(object); // Throwables thrown in here shall not be catched!
    }

    private void execute0(URL url, Observer observer) {
        checkEngineSet();
        Reader reader = null;
        Object object;
        try {
            reader = new InputStreamReader(url.openStream());
            object = engine.eval(reader);
        } catch (Throwable e) {
            observer.onFailure(e);
            return;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        observer.onSuccess(object); // Throwables thrown in here shall not be catched!
    }

    private ScriptEngine getEngine(Callable<ScriptEngine> task) {
        try {
            return executorService.submit(task).get();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private void configureEngine() {

        ScriptContext context = new SimpleScriptContext();
        context.setWriter(output);
        context.setErrorWriter(output);

        engine.setContext(context);
        engine.put("out", output);
        engine.put("err", output);

        output.println(MessageFormat.format("Script language set to {0}.", engine.getFactory().getLanguageName()));

        final URL url = findInitScript();
        if (url != null) {
            output.println(MessageFormat.format("Loading initialisation script ''{0}''...", url));
            execute0(url, new Observer() {
                @Override
                public void onSuccess(Object value) {
                }

                @Override
                public void onFailure(Throwable throwable) {
                    output.println("Failed to load initialisation script. " +
                            "BEAM-specific language extensions are disabled.");
                    throwable.printStackTrace(output);
                }
            });
            output.println("Initialisation script loaded. BEAM-specific language extensions are enabled.");
        } else {
            output.println("No initialisation script found. " +
                    "BEAM-specific language extensions are disabled.");
        }
    }

    private URL findInitScript() {
        String cl = getClass().getSimpleName();
        String ln = engine.getFactory().getLanguageName();
        URL url = findInitScript(cl + "_" + ln + ".%s");
        if (url == null) {
            return findInitScript(cl + ".%s");
        }
        return null;
    }

    private URL findInitScript(String pattern) {
        for (String extension : engine.getFactory().getExtensions()) {
            URL resource = getClass().getResource(String.format(pattern, extension));
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    private void checkEngineSet() {
        if (engine == null) {
            throw new IllegalStateException("engine == null");
        }
    }

    public void reset() {
        executorService.shutdownNow();
        executorService = createExecutorService();
    }

    private ExecutorService createExecutorService() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ScriptRunner");
            thread.setContextClassLoader(ScriptManager.this.classLoader);
            return thread;
        });
    }

    public static interface Observer {
        void onSuccess(Object value);

        void onFailure(Throwable throwable);
    }

}
