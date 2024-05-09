/*
 *
 *  *
 *  *
 *  *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  *  * You may not use this work except in compliance with the Licence.
 *  *  * You may obtain a copy of the Licence at:
 *  *  *
 *  *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the Licence for the specific language governing permissions and
 *  *  * limitations under the Licence.
 *  *
 *
 */

package org.entur.gbfs.validator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Loader {

    public Map<String, InputStream> load(String discoveryURI) {
        Map<String, InputStream> result = new HashMap<>();
        InputStream discoveryFileStream = loadFile(URI.create(discoveryURI));

        result.put("gbfs", discoveryFileStream);

        JSONObject discoveryFile = new JSONObject(new JSONTokener(discoveryFileStream));

        String version = (String) discoveryFile.get("version");

        JSONArray files;

        if (version.matches("^3\\.\\d")) {
            files = getV3Files(discoveryFile);
        } else {
            files = getPreV3Files(discoveryFile);
        }

        files.forEach(file -> {
            JSONObject fileObj = (JSONObject) file;
            String fileName = (String) fileObj.get("name");
            String fileURL = (String) fileObj.get("url");
            InputStream fileStream = loadFile(URI.create(fileURL));
            result.put(fileName, fileStream);
        });

        return result;
    }

    private JSONArray getV3Files(JSONObject discoveryFile) {
        return discoveryFile.getJSONObject("data").getJSONArray("feeds");
    }

    private JSONArray getPreV3Files(JSONObject discoveryFile) {
        String firstLanguageKey = discoveryFile.getJSONObject("data").keys().next();
        return discoveryFile.getJSONObject("data").getJSONObject(firstLanguageKey).getJSONArray("feeds");
    }

    private InputStream loadFile(URI fileURI) {
        if (fileURI.getScheme().equals("file")) {
            try {
                return getFileInputStream(fileURI);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if (fileURI.getScheme().equals("https") || fileURI.getScheme().equals("http")) {
            try {
                return getHTTPInputStream(fileURI);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException("Scheme not supported");
    }

    private static FileInputStream getFileInputStream(URI fileURI) throws FileNotFoundException {
        return new FileInputStream(new File(fileURI));
    }

    private InputStream getHTTPInputStream(URI fileURI) throws IOException {
        URL url = fileURI.toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setRequestProperty("Et-Client-Name", "entur-gbfs-validator");
        con.connect();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));

        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        return new ByteArrayInputStream(content.toString().getBytes());
    }
}
