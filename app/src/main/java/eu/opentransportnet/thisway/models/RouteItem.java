package eu.opentransportnet.thisway.models;

/**
 * @author Kristaps Krumins
 */
public class RouteItem {
    private String routeId;
    private String startAddress;
    private String endAddress;
    private String localFileName;

    public RouteItem(String routeId, String startAddress, String endAddress) {
        this.routeId = routeId;
        this.startAddress = startAddress;
        this.endAddress = endAddress;
    }

    public RouteItem(String routeId, String startAddress, String endAddress, String fileName) {
        this.routeId = routeId;
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        localFileName = fileName;
    }

    public String getRouteId() {
        return routeId;
    }

    @Override
    public String toString() {
        return startAddress + " \u2192 " + endAddress;
    }

    public String getLocalFileName() {
        if (localFileName == null || localFileName.isEmpty()) {
            return null;
        }

        return localFileName;
    }
}
