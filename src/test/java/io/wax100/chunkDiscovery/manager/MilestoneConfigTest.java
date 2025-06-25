package io.wax100.chunkDiscovery.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneConfigTest {

    @Mock
    private Logger mockLogger;

    private MilestoneConfig milestoneConfig;
    private YamlConfiguration testConfig;

    @BeforeEach
    void setUp() {
        milestoneConfig = new MilestoneConfig(mockLogger);
        
        String configYml = """
            rewards:
              milestones:
                5:
                  item:
                    material: IRON_INGOT
                    amount: 2
                  experience: 25
                  send_message: true
                  broadcast: false
                  play_effects: true
                  message: "5チャンク発見おめでとう！"
                10:
                  item:
                    material: GOLD_INGOT
                    amount: 1
                  experience: 50
                  send_message: true
                  broadcast: true
                  play_effects: true
                  message: "10チャンク発見おめでとう！"
                25:
                  item:
                    material: DIAMOND
                    amount: 1
                  experience: 100
            """;
        
        testConfig = YamlConfiguration.loadConfiguration(new StringReader(configYml));
    }

    @Test
    void testLoadConfiguration_Success() {
        // Act
        milestoneConfig.loadConfiguration(testConfig);

        // Assert
        List<MilestoneConfig.MilestoneEntry> personalMilestones = milestoneConfig.getPersonalMilestones();
        assertEquals(3, personalMilestones.size());
        
        // Test sorting
        assertEquals(5, personalMilestones.get(0).discoveryCount);
        assertEquals(10, personalMilestones.get(1).discoveryCount);
        assertEquals(25, personalMilestones.get(2).discoveryCount);
        
        verify(mockLogger).info("マイルストーン設定を読み込みました: 3個");
    }

    @Test
    void testLoadConfiguration_NoMilestonesSection() {
        // Arrange
        YamlConfiguration emptyConfig = new YamlConfiguration();

        // Act
        milestoneConfig.loadConfiguration(emptyConfig);

        // Assert
        assertTrue(milestoneConfig.getPersonalMilestones().isEmpty());
        verify(mockLogger).warning("マイルストーン設定が見つかりません");
    }

    @Test
    void testGetMilestone_Found() {
        // Arrange
        milestoneConfig.loadConfiguration(testConfig);

        // Act
        Optional<MilestoneConfig.MilestoneEntry> milestone = milestoneConfig.getMilestone(10, false);

        // Assert
        assertTrue(milestone.isPresent());
        assertEquals(10, milestone.get().discoveryCount);
        assertEquals("10チャンク発見おめでとう！", milestone.get().message);
        assertTrue(milestone.get().broadcast);
    }

    @Test
    void testGetMilestone_NotFound() {
        // Arrange
        milestoneConfig.loadConfiguration(testConfig);

        // Act
        Optional<MilestoneConfig.MilestoneEntry> milestone = milestoneConfig.getMilestone(100, false);

        // Assert
        assertFalse(milestone.isPresent());
    }

    @Test
    void testGlobalMilestones_CopyOfPersonal() {
        // Arrange
        milestoneConfig.loadConfiguration(testConfig);

        // Act
        List<MilestoneConfig.MilestoneEntry> personalMilestones = milestoneConfig.getPersonalMilestones();
        List<MilestoneConfig.MilestoneEntry> globalMilestones = milestoneConfig.getGlobalMilestones();

        // Assert
        assertEquals(personalMilestones.size(), globalMilestones.size());
        for (int i = 0; i < personalMilestones.size(); i++) {
            assertEquals(personalMilestones.get(i).discoveryCount, 
                        globalMilestones.get(i).discoveryCount);
        }
    }

    @Test
    void testStaticListIntegration_Size() {
        // Arrange
        milestoneConfig.loadConfiguration(testConfig);
        MilestoneConfig.setInstance(milestoneConfig);

        // Act & Assert
        assertEquals(3, MilestoneConfig.personal.size());
        assertEquals(3, MilestoneConfig.global.size());
    }

    @Test
    void testStaticListIntegration_Iterator() {
        // Arrange
        milestoneConfig.loadConfiguration(testConfig);
        MilestoneConfig.setInstance(milestoneConfig);

        // Act
        int count = 0;
        for (MilestoneConfig.MilestoneEntry entry : MilestoneConfig.personal) {
            count++;
            assertNotNull(entry);
            assertTrue(entry.discoveryCount > 0);
        }

        // Assert
        assertEquals(3, count);
    }

    @Test
    void testStaticListIntegration_Get() {
        // Arrange
        milestoneConfig.loadConfiguration(testConfig);
        MilestoneConfig.setInstance(milestoneConfig);

        // Act & Assert
        MilestoneConfig.MilestoneEntry firstMilestone = MilestoneConfig.personal.get(0);
        assertEquals(5, firstMilestone.discoveryCount);
        assertEquals("5チャンク発見おめでとう！", firstMilestone.message);
        assertFalse(firstMilestone.broadcast);
    }

    @Test
    void testStaticListIntegration_IsEmpty() {
        // Arrange - No configuration loaded
        MilestoneConfig.setInstance(milestoneConfig);

        // Act & Assert
        assertTrue(MilestoneConfig.personal.isEmpty());
        assertTrue(MilestoneConfig.global.isEmpty());
    }

    @Test
    void testMilestoneEntryDefaults() {
        // Arrange
        String minimalConfigYml = """
            rewards:
              milestones:
                50:
                  item:
                    material: NETHERITE_INGOT
                    amount: 1
                  experience: 200
            """;
        
        YamlConfiguration minimalConfig = YamlConfiguration.loadConfiguration(new StringReader(minimalConfigYml));

        // Act
        milestoneConfig.loadConfiguration(minimalConfig);

        // Assert
        Optional<MilestoneConfig.MilestoneEntry> milestone = milestoneConfig.getMilestone(50, false);
        assertTrue(milestone.isPresent());
        
        MilestoneConfig.MilestoneEntry entry = milestone.get();
        assertTrue(entry.sendMessage); // default true
        assertFalse(entry.broadcast); // default false for < 100
        assertTrue(entry.playEffects); // default true
        assertEquals("50チャンク発見達成！", entry.message); // default message
    }
}