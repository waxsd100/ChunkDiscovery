package io.wax100.chunkDiscovery.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class MilestoneConfig {
    public static List<MilestoneEntry> personal;
    public static List<MilestoneEntry> global;

    public static void init(Path dataFolder) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(dataFolder.resolve("milestones.json").toFile())) {
            Type listType = new TypeToken<List<MilestoneEntry>>(){}.getType();
            personal = gson.fromJson(reader, listType);
        } catch (Exception e) {
            personal = Collections.emptyList();
        }
        try (FileReader reader = new FileReader(dataFolder.resolve("global_milestones.json").toFile())) {
            Type listType = new TypeToken<List<MilestoneEntry>>(){}.getType();
            global = gson.fromJson(reader, listType);
        } catch (Exception e) {
            global = Collections.emptyList();
        }
    }

    public static class MilestoneEntry {
        public int discoveryCount;
        public List<io.wax100.chunkDiscovery.model.RewardItem> items;
        public String message;
        public boolean sendMessage = true;
        public boolean broadcast = false;
        public boolean playEffects = false;
    }
}