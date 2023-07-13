package com.example;

import de.westnordost.osmapi.map.data.Element;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.*;
@AllArgsConstructor
@NoArgsConstructor
public class OverpassQueryResult {

     List<Relation>  relations;
     List<Element> elements;
     private List<Node> nodes;
     private List<City> cities;

     public OverpassQueryResult(List<Relation> relations) {
          this.relations = relations;
     }

     public List<Relation> getRelations() {
          return relations;
     }

     public void setRelations(List<Relation> relations) {
          this.relations = relations;
     }

     public List<City> getCities() {
          return cities;
     }

     public void setCities(List<City> cities) {
          this.cities = cities;
     }

     public List<Element> getElements() {
          return elements;
     }

     public void setElements(List<Element> elements) {
          this.elements = elements;
     }

     public List<Node> getNodes() {
          return nodes;
     }

     public void setNodes(List<Node> nodes) {
          this.nodes = nodes;
     }
}
