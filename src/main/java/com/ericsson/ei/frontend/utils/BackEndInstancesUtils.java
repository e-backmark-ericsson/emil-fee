/*
   Copyright 2018 Ericsson AB.
   For a full list of individual contributors, please see the commit history.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.ericsson.ei.frontend.utils;

import com.ericsson.ei.frontend.model.BackEndInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
public class BackEndInstancesUtils {

    private static final Logger LOG = LoggerFactory.getLogger(BackEndInstancesUtils.class);
    private static final String PATH_TO_WRITE = "src/main/resources/EIBackendInstancesInformation.json";
    private static final String NAME = "name";
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String PATH = "path";
    private static final String HTTPS = "https";
    private static final String ACTIVE = "active";

    @Value("${ei.backendServerHost:#{null}}")
    private String host;

    @Value("${ei.backendServerPort:#{null}}")
    private String port;

    @Value("${ei.backendContextPath:#{null}}")
    private String path;

    @Value("${ei.useSecureHttpBackend:#{false}}")
    private boolean useSecureHttpBackend;

    @Value("${ei.backendInstancesPath:#{null}}")
    private String eiInstancesPath;

    @Autowired
    private BackEndInformation backEndInformation;

    private List<BackEndInformation> information = new ArrayList<>();
    private JsonArray instances = new JsonArray();

    @PostConstruct
    public void init() {
        if (eiInstancesPath == null || eiInstancesPath.isEmpty()) {
            setEiInstancesPath(PATH_TO_WRITE);
        }
        parseBackEndInstancesFile();
        if (getCurrentInstance() != null && !checkIfInstanceAlreadyExist(getCurrentInstance())) {
            instances.add(getCurrentInstance());
        }
        writeIntoFile();
        information.clear();
        information = new Gson().fromJson(instances, new TypeToken<List<BackEndInformation>>() {
        }.getType());
        for (BackEndInformation backEndInformation : information) {
            if (backEndInformation.isActive()) {
                setBackEndProperties(backEndInformation);
            }
        }
    }

    private JsonObject getCurrentInstance() {
        if (host != null && port != null && !host.isEmpty() && !port.isEmpty()) {
            JsonObject instance = new JsonObject();
            instance.addProperty(NAME, "default");
            instance.addProperty(HOST, host);
            instance.addProperty(PORT, Integer.valueOf(port));
            instance.addProperty(PATH, path);
            instance.addProperty(HTTPS, useSecureHttpBackend);
            instance.addProperty(ACTIVE, true);
            return instance;
        } else
            return null;
    }

    public void setBackEndProperties(BackEndInformation properties) {
        backEndInformation.setName(properties.getName());
        backEndInformation.setHost(properties.getHost());
        backEndInformation.setPort(properties.getPort());
        backEndInformation.setPath(properties.getPath());
        backEndInformation.setUseSecureHttpBackend(properties.isUseSecureHttpBackend());
    }

    public boolean checkIfInstanceAlreadyExist(JsonObject instance) {
        for (JsonElement element : instances) {
            if (element.getAsJsonObject().get(HOST).equals(instance.get(HOST)) &&
                    element.getAsJsonObject().get(PORT).getAsInt() == instance.get(PORT).getAsInt() &&
                    element.getAsJsonObject().get(PATH).equals(instance.get(PATH)) &&
                    element.getAsJsonObject().get(NAME).equals(instance.get(NAME))) {
                return true;
            }
        }
        return false;
    }

    public void writeIntoFile() {
        try {
            Files.write(Paths.get(eiInstancesPath), instances.toString().getBytes());
            parseBackEndInstancesFile();
        } catch (IOException e) {
            LOG.error("Couldn't add instance to file " + e.getMessage());
        }
    }

    private void parseBackEndInstancesFile() {
        try {
            information.clear();
            instances = new JsonArray();
            JsonArray inputBackEndInstances = new JsonParser().parse(new String(Files.readAllBytes(Paths.get(eiInstancesPath)))).getAsJsonArray();
            for (JsonElement element : inputBackEndInstances) {
                information.add(new ObjectMapper().readValue(element.toString(), BackEndInformation.class));
                instances.add(element);
            }
        } catch (IOException e) {
            LOG.error("Failure when try to parse json file" + e.getMessage());
        }
    }
}