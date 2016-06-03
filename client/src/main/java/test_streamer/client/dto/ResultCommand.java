package test_streamer.client.dto;

import us.bpsm.edn.Keyword;

import java.util.UUID;

/**
 * A command for :result.
 *
 * @author kawasima
 */
public class ResultCommand {
    private Keyword command = Keyword.newKeyword("result");
    private String name;
    private UUID shotId;
    private TestSuiteResult result;

    public ResultCommand(String name, UUID shotId) {
        this.name = name;
        this.shotId = shotId;
    }

    public Keyword getCommand() {

        return command;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getShotId() {
        return shotId;
    }

    public void setShotId(UUID shotId) {
        this.shotId = shotId;
    }

    public TestSuiteResult getResult() {
        return result;
    }

    public void setResult(TestSuiteResult result) {
        this.result = result;
    }

}
