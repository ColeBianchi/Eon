package com.colebianchi.apps.Eon;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import com.colebianchi.apps.Eon.vservice.VhostsService;

public class QuickStartTileService extends TileService {

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        if (tile==null)return;
        int state = tile.getState();
        if (state == Tile.STATE_ACTIVE) {
            tile.setState(Tile.STATE_INACTIVE);
            VhostsService.stopVService(this.getApplicationContext());
       } else if(state == Tile.STATE_INACTIVE){
            tile.setState(Tile.STATE_ACTIVE);
            VhostsService.startVService(this.getApplicationContext(),1);
        }else{
            tile.setState(Tile.STATE_UNAVAILABLE);
        }
        tile.updateTile();
    }
    @Override
    public void onStartListening () {
        Tile tile = getQsTile();
        if (tile==null)return;
        if(VhostsService.isRunning()){
            tile.setState(Tile.STATE_ACTIVE);
        }else{
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }

}
