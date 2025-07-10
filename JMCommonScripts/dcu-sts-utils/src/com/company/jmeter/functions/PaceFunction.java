package com.company.jmeter.functions;

import com.company.jmeter.setup.GroupInitUtil;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.*;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.util.*;

/**
 * ${__PACE()} – Apply pacing logic at the top of a loop.
 * Optional first argument supplies the pacing spec (same syntax as the
 * Parameters field in the old Groovy version, e.g. "75" or "60,90").
 */
public final class PaceFunction extends AbstractFunction {

    private static final Logger LOG = LoggingManager.getLoggerForClass();
    private static final List<String> DESC = List.of("Pacing spec (single value or min,max)");

    private Collection<CompoundVariable> params;

    @Override
    public void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
        // zero or one parameter allowed
        if (parameters.size() > 1) throw new InvalidVariableException("__PACE takes zero or one argument");
        this.params = parameters;
    }

    @Override
    public String execute(SampleResult prev, Sampler curr) throws InvalidVariableException {
        String spec = params.isEmpty() ? null : params.iterator().next().execute();
        JMeterVariables vars = getVariables();
        GroupInitUtil.applyPacing(LOG, vars, JMeterContextService.getContext(), spec);
        return ""; // nothing to replace in-line
    }

    @Override public String getReferenceKey() { return "__PACE"; }
    @Override public List<String> getArgumentDesc() { return DESC; }
} 