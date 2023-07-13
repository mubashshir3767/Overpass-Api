package com.example;


import com.fasterxml.jackson.databind.json.JsonMapper;
import de.westnordost.osmapi.ApiResponseReader;
import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.map.data.Element;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.overpass.OverpassMapDataDao;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.example.City.toPascalCase;

public class Main {
     static HttpURLConnection con = null;

     public static void main(String[] args) throws IOException {
          URL url = new URL("https://overpass-api.de/api/interpreter");
          con = (HttpURLConnection) url.openConnection();
          con.setRequestMethod("GET");
          con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");

          System.out.println("User-Agent: " + con.getRequestProperty("User-Agent"));
          int limitCities = getInt(args, 0);
          int limitStreets = getInt(args, 1);
          getResultsAndSave(limitCities, limitStreets);
     }

     public static int getInt(String[] arr, int index) {
          if (index >= arr.length) {
               return 0; // or return some default value
          }
          try {
               return Integer.parseInt(arr[index]);
          } catch (NumberFormatException e) {
               return 0; // or return some default value
          }
     }

     public static OverpassQueryResult downloadWithRetry(String query, int retries) {
          OverpassMapDataDao overpassMapDataDao = new OverpassMapDataDao(new OsmConnection(con.getURL().toString(), con.getRequestProperty("User-Agent")));
          if (retries == 0) {
               return null;
          }
          try {
               return overpassMapDataDao.query(query, new ApiResponseReader<OverpassQueryResult>() {
                    @Override
                    public OverpassQueryResult parse(InputStream in) throws Exception {

//                         JsonMapper jsonMapper = new JsonMapper();
//                         byte[] clone = in.readAllBytes();
//                         String xmlString = new String(clone, "UTF-8");
//                         var jsonObject = XML.toJSONObject(xmlString);
//                         System.out.println(jsonObject);


//                         JAXBContext jaxbContext = JAXBContext.newInstance(OverpassQueryResult.class);
//
//                         Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//
//                         OverpassQueryResult result = (OverpassQueryResult) jaxbUnmarshaller.unmarshal(in);
//
//                         System.out.println(result);

//
                         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                         DocumentBuilder builder = factory.newDocumentBuilder();
                         Document doc = builder.parse(in);

                         NodeList nodeList = doc.getElementsByTagName("node");
                         List<Node> nodes = new ArrayList<>();

                         for (int i = 0; i < nodeList.getLength(); i++) {
                              org.w3c.dom.Node item = nodeList.item(i);
                              Node node = new Node();
                              node.setId(item.getAttributes().getNamedItem("id").getNodeValue());
                              node.setLat(item.getAttributes().getNamedItem("lat").getNodeValue());
                              node.setLon(item.getAttributes().getNamedItem("lon").getNodeValue());

                              NodeList tagList = item.getChildNodes();
                              for (int j = 0; j < tagList.getLength(); j++) {
                                   org.w3c.dom.Node item1 = tagList.item(j);
                                   if (item1.getNodeType() == item1.ELEMENT_NODE) {
                                        Node.Tag tag = new Node.Tag();
                                        tag.setK(item1.getAttributes().getNamedItem("k").getNodeValue());
                                        tag.setV(item1.getAttributes().getNamedItem("v").getNodeValue());
                                        node.getTags().add(tag);
                                   }
                              }
                              nodes.add(node);
                         }
                         int i=0;
                         for (Node node : nodes) {
                              for (Node.Tag tag : node.getTags()) {
                                   if (tag.getK().equals("name")||tag.getK().equals("city")||tag.getK().equals("postcode")||tag.getK().equals("COMMUN_NAM")||tag.getK().equals("PREFECTURE")) {
                                        System.out.print(tag.getV()+", ");
                                         i++;
                                   }
                                   if (i==5){
                                        i=0;
                                        System.out.println();
                                   }
                              }
                         }

                         return null;
                    }
               });
          } catch (Exception e) {
               System.out.printf("Download for query %s failed. Retrying...\n", query);
               return downloadWithRetry(query, retries - 1);
          }
     }


     public static List<City> getCities() {
          OverpassQueryResult result = downloadWithRetry(
              "area[\"ISO3166-1:alpha2\"=\"AL\"];\n" +
                  "node[place~\"city|town|village\"](area);\n" +
                  "out;",
              10
          );
          if (result == null) {
               return null;
          }

          List<City> cities = result.getNodes().stream()
              .map(City::new)
              .filter(city -> city.getName() != null)
              .distinct()
              .collect(Collectors.toList());

          System.out.printf("Search returned %d cities in Albania.\n", cities.size());

          for (City city : cities) {
               city.fill();
          }
          return cities;
     }


     public static Predicate<Element> createRelationChecker(String cityName) {
          return (Element x) -> x instanceof Relation &&
              x.getTags().containsKey("postal_code") &&
              x.getTags().get("name").equals(cityName);
     }

     public static String getPostalCodeFor(City city) {
          OverpassMapDataDao overpassMapDataDao = new OverpassMapDataDao(new OsmConnection(con.getURL().toString(), con.getRequestProperty("User-Agent")));

          assert city != null;
          try {
               OverpassQueryResult result = overpassMapDataDao.query(
                   String.format("relation[name=\"%s\"];\nout;", city.getName()), in -> null);

               Predicate<Element> checker = createRelationChecker(city.getName());
               List<Relation> relations = result.getRelations().stream()
                   .filter(checker)
                   .map(x -> x)
                   .collect(Collectors.toList());
               if (!relations.isEmpty()) {
                    String postalCode = relations.get(0).getTags().get("postal_code");
                    System.out.printf("Found postal code %s for city %s\n", postalCode, city.getName());
                    return postalCode;
               } else {
                    System.out.printf("No postal code found for city %s.\n", city.getName());
                    return null;
               }
          } catch (Exception e) {
               System.out.printf("Postal code search request failed for city %s. Skipping...\n", city.getName());
               return null;
          }
     }

     public static List<Map<String, String>> getStreetsFor(City city, Integer limit) {
          List<Map<String, String>> streets = new ArrayList<>();
          try {
               OverpassQueryResult result = downloadWithRetry(
                   String.format(
                       "area[name=\"%s\"];\nway(area)[highway][name];\nout;",
                       city.getName()
                   ),
                   10
               );

               if (result == null) return streets;

               Set<String> streetnames = new HashSet<>();
               for (Element element : result.getElements()) {
                    if (element instanceof Way way) {
                         String streetname = way.getTags().get("name");
                         if (streetname != null) {
                              streetnames.add(streetname);
                         }
                    }
               }

               List<String> streetnameList = new ArrayList<>(streetnames);
               if (limit == null) {
                    limit = streetnames.size();
               }
               int numStreets = Math.min(streetnameList.size(), limit);
               System.out.printf("Search for streets in %s returned %d streets.\n", city.getName(), numStreets);

               for (String streetname : streetnameList.subList(0, numStreets)) {
                    Map<String, String> street = new HashMap<>();
                    street.put("name", toPascalCase(streetname));
                    street.put("city", city.getName());
                    street.put("postcode", getPostalCodeFor(city));
                    street.put("region1", city.getDistrict());
                    street.put("region2", city.getPrefecture());
                    street.put("region3", city.getRegion());
                    System.out.println(street);
                    streets.add(street);
               }

               int numPostcodes = (int) streets.stream().filter(s -> s.get("postcode") != null).count();
               System.out.printf("Found %d postcodes out of %d.\n", numPostcodes, streets.size());

          } catch (Exception e) {
               e.printStackTrace();
          }

          return streets;
     }


     public static List<Map<String, String>> getStreetsForMultiple(Iterable<City> cities, Integer limitCities, Integer limitStreets) {
          List<Map<String, String>> streets = new ArrayList<>();
          int i = 0;
          for (City city : cities) {
               if (i == limitCities) break;
               List<Map<String, String>> cityStreets = getStreetsFor(city, limitStreets);
               streets.addAll(cityStreets);
               i += 1;
          }
          return streets;
     }


     public static void getResultsAndSave(Integer limit_cities, Integer limit_streets) {
          List<City> cities = getCities();
          if (cities == null) {
               System.out.println("Couldn't download data for cities. Exiting...");
               return;
          }
          List<Map<String, String>> streets = getStreetsForMultiple(cities, limit_cities, limit_streets);
          if (streets.size() > 0) {
               System.out.printf("Saving %d rows to results.csv...\n", streets.size());
               try (FileWriter writer = new FileWriter("results.csv")) {
                    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(streets.get(0).keySet().toArray(new String[0])));
                    for (Map<String, String> street : streets) {
                         csvPrinter.printRecord(street.values());
                    }
                    csvPrinter.flush();
               } catch (IOException e) {
                    e.printStackTrace();
               }
               System.out.println("Done.");
          } else {
               System.out.println("No streets to write. Exiting...");
          }
     }

}
