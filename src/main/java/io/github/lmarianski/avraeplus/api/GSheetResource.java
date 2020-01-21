package io.github.lmarianski.avraeplus.api;

import io.github.lmarianski.avraeplus.GSheetsClient;
import io.github.lmarianski.avraeplus.Main;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import java.io.IOException;
import java.util.List;

@Path("gsheet")
public class GSheetResource {

    @Path("{sheetId}")
    @GET
    @Produces("text/plain")
    public String getRange(@PathParam("sheetId") final String sheetId, @QueryParam("range") final String range) throws IOException {
        List<List<Object>> o = GSheetsClient.getRange(sheetId, range).getValues();
        return Main.gson.toJson(o);
    }
}
