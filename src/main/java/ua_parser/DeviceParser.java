/**
 * Copyright 2012 Twitter, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ua_parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Device parser using ua-parser regexes. Extracts device information from user agent strings.
 *
 * @author Steve Jiang (@sjiang) <gh at iamsteve com>
 */
public class DeviceParser {
  List<DevicePattern> patterns;

  public DeviceParser(List<DevicePattern> patterns) {
    this.patterns = patterns;
  }

  public Device parse(String agentString) {
    if (agentString == null) {
      return null;
    }

    Device device = null;
    for (DevicePattern p : patterns) {
      if ((device = p.match(agentString)) != null) {
        break;
      }
    }
    if (device == null)
      return new Device("Other", null, null);

    return device;
  }

  public static DeviceParser fromList(List<Map<String,String>> configList) {
    List<DevicePattern> configPatterns = new ArrayList<DevicePattern>();
    for (Map<String,String> configMap : configList) {
      configPatterns.add(DeviceParser.patternFromMap(configMap));
    }
    return new DeviceParser(configPatterns);
  }

  protected static DevicePattern patternFromMap(Map<String, String> configMap) {
    String regex = configMap.get("regex");
    if (regex == null) {
      throw new IllegalArgumentException("Device is missing regex");
    }    
    Pattern pattern = "i".equals(configMap.get("regex_flag")) // no ohter flags used (by now) 
    		? Pattern.compile(regex, Pattern.CASE_INSENSITIVE) : Pattern.compile(regex);
    return new DevicePattern(pattern, configMap.get("device_replacement"),
        configMap.get("brand_replacement"), configMap.get("model_replacement"));
  }

  protected static class DevicePattern {
	private static final Pattern SUBSTITUTIONS_PATTERN = Pattern.compile("\\$\\d");
    private final Pattern pattern;
    private final String deviceReplacement;
    private final String brandReplacement;
    private final String modelReplacement;

    public DevicePattern(Pattern pattern, String deviceReplacement, String brandReplacement,
        String modelReplacement) {
      this.pattern = pattern;
      this.deviceReplacement = deviceReplacement;
      this.brandReplacement = brandReplacement;
      this.modelReplacement = modelReplacement;
    }

    public Device match(String agentString) {
      Matcher matcher = pattern.matcher(agentString);
      if (!matcher.find()) {
        return null;
      }
      String device, brand = null, model = null;
      if (deviceReplacement != null) {
        device = multiReplace(deviceReplacement, matcher);
      } else {
        device = matcher.group(1);
      }

      if (brandReplacement != null) {
        brand = multiReplace(brandReplacement, matcher);
      }

      if (modelReplacement != null) {
        model = multiReplace(modelReplacement, matcher);
      } else if (matcher.groupCount() >= 1) {
        model = matcher.group(1);
      }

      if (device == null || device.equals(""))
        device = "Other";

      if ((brand != null) && brand.equals(""))
        brand = null;

      if ((model != null) && model.equals(""))
        model = null;

      return new Device(device, brand, model);
    }
    
    private String multiReplace(String typeReplacement, Matcher matcher) {
      String ret = null;
      if (typeReplacement.contains("$")) {
        ret = typeReplacement;
        for (String substitution : getSubstitutions(typeReplacement)) {
          int i = Integer.valueOf(substitution.substring(1));
          String replacement = matcher.groupCount() >= i && matcher.group(i) != null
              ? Matcher.quoteReplacement(matcher.group(i)) : "";
          ret = ret.replaceFirst("\\" + substitution, replacement);
        }
        ret = ret.trim();
      } else {
        ret = typeReplacement;
      }
      return ret;
    }

    private List<String> getSubstitutions(String typeReplacement) {
      Matcher matcher = SUBSTITUTIONS_PATTERN.matcher(typeReplacement);
      List<String> substitutions = new ArrayList<String>();
      while (matcher.find()) {
        substitutions.add(matcher.group());
      }
      return substitutions;
    }
    
  }

}