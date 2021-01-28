package io.github.lmarianski.avraeplus.rest;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class RESTApplication extends Application<RESTConfiguration> {
    @Override
    public void run(RESTConfiguration configuration, Environment environment) throws Exception {
        final RESTScaleImageResource resource = new RESTScaleImageResource();
        environment.jersey().register(resource);
    }
}
