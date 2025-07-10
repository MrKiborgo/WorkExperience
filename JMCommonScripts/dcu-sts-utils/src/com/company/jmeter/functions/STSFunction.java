package com.company.jmeter.functions;

import java.util.*;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.*;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.company.STS;

/**
 * JMeter function  ${__STS(cmdLine[,varName])}
 * Example:  ${__STS(KEEP,applications.csv,APP_ID,Passport_Number)}           – discard result
 *           ${__STS(DEL,users.csv,USER_ID,flag)}   → ${flag} == "true|false"
 */
public final class STSFunction extends AbstractFunction {

    private static final Logger LOG  = LoggingManager.getLoggerForClass();
    private static final List<String> DESC =
            List.of("STS command string",
                    "Optional var to capture true/false");

    private Collection<CompoundVariable> params;

    @Override
    public void setParameters(Collection<CompoundVariable> parameters)
                               throws InvalidVariableException {
        checkMinParameterCount(parameters, 1);
        params = parameters;
    }

    @Override
    public String execute(SampleResult prev, Sampler curr)
                          throws InvalidVariableException {

        Iterator<CompoundVariable> it = params.iterator();
        String cmdLine = it.next().execute();
        String outVar  = it.hasNext() ? it.next().execute() : null;

        JMeterVariables vars = getVariables();
        boolean ok = STS.exec(LOG, vars, JMeterUtils.getJMeterProperties(), cmdLine);

        if (outVar != null && vars != null) {
            vars.put(outVar, String.valueOf(ok));
        }
        return "";                      // no in-line replacement
    }

    @Override public String getReferenceKey()  { return "__STS"; }
    @Override public List<String> getArgumentDesc() { return DESC; }
}
