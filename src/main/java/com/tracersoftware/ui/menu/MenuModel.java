package com.tracersoftware.ui.menu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MenuModel {

    public static class MenuManifest {
        private String title;
        private List<MenuItem> items;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<MenuItem> getItems() {
            return items;
        }

        public void setItems(List<MenuItem> items) {
            this.items = items;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MenuItem {
        private String id;
        private String label;
        private String icon;
        private String route;
        private String action;
        @JsonProperty("children")
        private List<MenuItem> children;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getRoute() {
            return route;
        }

        public void setRoute(String route) {
            this.route = route;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public List<MenuItem> getChildren() {
            return children;
        }

        public void setChildren(List<MenuItem> children) {
            this.children = children;
        }
    }
}
