package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.overpass.OverpassMapDataDao;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.Main.con;

@NoArgsConstructor
@AllArgsConstructor
public class City {

     private String name;
     private String district;
     private String prefecture;
     private String region;
     private Node node;

     public City(Node node) {
          this.name = node.getTags().get("name");
          this.district = null;
          this.prefecture = null;
          this.region = null;
          this.node = node;
     }

     public String getName() {
          return this.name;
     }

     public void setName(String name) {
          this.name = name;
     }

     public void setDistrict(String district) {
          this.district = district;
     }

     public void setPrefecture(String prefecture) {
          this.prefecture = prefecture;
     }

     public void setRegion(String region) {
          this.region = region;
     }

     public void setNode(Node node) {
          this.node = node;
     }

     public String getDistrict() {
          return this.district;
     }

     public String getPrefecture() {
          return this.prefecture;
     }

     public String getRegion() {
          return this.region;
     }

     public Node getNode() {
          return this.node;
     }


     //=============================================================

     public static Object get(List<Object> l, int num) {
          return num < l.size() ? l.get(num) : null;
     }

     public static Integer getInt(List<Object> l, int num) {
          Object val = get(l, num);
          return val != null ? Integer.parseInt(val.toString()) : null;
     }

     public static String toPascalCase(String name) {
          return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
     }



     public void fill() {
          this.getDistrict1();
          this.getPrefecture1();
          this.getRegion1();
     }

     public void getDistrict1() {
          String dst = this.node.getTags().get("addr:district");
          this.district = (toPascalCase(dst));
     }


     private void getPrefecture1() {
          List<String> names = Arrays.asList(this.node.getTags().get("prefecture"), this.node.getTags().get("PREFECTURE"));
          String prefecture = (String) get(names.stream().filter(Objects::nonNull).collect(Collectors.toList()), 0);
          assert prefecture != null;
          this.prefecture = toPascalCase(prefecture);
     }


     private void getRegion1() {
          try {
               OverpassMapDataDao overpassMapDataDao = new OverpassMapDataDao(new OsmConnection(con.getURL().toString(), con.getRequestProperty("User-Agent")));
               ObjectMapper objectMapper = new ObjectMapper();
               OverpassQueryResult result = overpassMapDataDao.query(
                   String.format(
                       "is_in(%s, %s);relation(pivot)[boundary=\"administrative\"][admin_level=4];out;",
                       this.node.getPosition().getLatitude(),
                       this.node.getPosition().getLongitude()
                   ), in ->
                       objectMapper.readValue(in, OverpassQueryResult.class)
               );
               Relation relation = (Relation) get(Collections.singletonList(result.getRelations()), 0);
               this.region = relation != null ? relation.getTags().get("name") : null;
          } catch (Exception e) {
               System.out.printf("Couldn't download region for %s. Skipping...\n", this.name);
          }
     }


     @Override
     public String toString() {
          return String.format("City(name=\"%s\", district=\"%s\", region=\"%s\")", this.name, this.district, this.region);
     }

     @Override
     public int hashCode() {
          return Objects.hash(this.name);
     }

     @Override
     public boolean equals(Object right) {
          if (right == this) {
               return true;
          }
          if (!(right instanceof City)) {
               return false;
          }
          City city = (City) right;
          return Objects.equals(this.name, city.name);
     }
}

