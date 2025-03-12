package com.example.captive_portal_analyzer_kotlin.screens.analysis.pcap_capture.capture_service;

/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */


import android.view.LayoutInflater;

import com.example.captive_portal_analyzer_kotlin.CaptivePortalApp;
import com.google.android.material.chip.ChipGroup;

import java.io.Serializable;

public class FilterDescriptor implements Serializable {
    public ConnectionDescriptor.Status status;
    public boolean showMasked;
    public boolean onlyBlacklisted;
    public boolean onlyCleartext;
    public ConnectionDescriptor.FilteringStatus filteringStatus;
    public ConnectionDescriptor.DecryptionStatus decStatus;
    public String iface;
    public int uid = -2; // this is persistent and used internally (AppDetailsActivity)
    public long minSize = 0;

    public FilterDescriptor() {
        clear();
        assert(!isSet());
    }

    public boolean isSet() {
        return (status != ConnectionDescriptor.Status.STATUS_INVALID)
                || (decStatus != ConnectionDescriptor.DecryptionStatus.INVALID)
                || (filteringStatus != ConnectionDescriptor.FilteringStatus.INVALID)
                || (iface != null)
                || onlyBlacklisted
                || onlyCleartext
                || (uid != -2)
                || (minSize > 0)
                || (!showMasked && !CaptivePortalApp.getInstance().getVisualizationMask().isEmpty());
    }

    public boolean matches(ConnectionDescriptor conn) {
        return (showMasked || !CaptivePortalApp.getInstance().getVisualizationMask().matches(conn))
                && (!onlyBlacklisted || conn.isBlacklisted())
                && (!onlyCleartext || conn.isCleartext())
                && ((status == ConnectionDescriptor.Status.STATUS_INVALID) || (conn.getStatus().equals(status)))
                && ((decStatus == ConnectionDescriptor.DecryptionStatus.INVALID) || (conn.getDecryptionStatus() == decStatus))
                && ((filteringStatus == ConnectionDescriptor.FilteringStatus.INVALID) || ((filteringStatus == ConnectionDescriptor.FilteringStatus.BLOCKED) == conn.is_blocked))
                && ((iface == null) || (CaptureService.getInterfaceName(conn.ifidx).equals(iface)))
                && ((uid == -2) || (uid == conn.uid))
                && ((minSize == 0) || ((conn.sent_bytes + conn.rcvd_bytes) >= minSize));
    }

    private void addChip(LayoutInflater inflater, ChipGroup group, int id, String text) {
        throw new UnsupportedOperationException("addChip method not properly implemented - TODO");
        /*Chip chip = (Chip) inflater.inflate(R.layout.active_filter_chip, group, false);
        chip.setId(id);
        chip.setText(text.toLowerCase());
        group.addView(chip);*/
    }

    public void toChips(LayoutInflater inflater, ChipGroup group) {
        throw new UnsupportedOperationException("addChip method not properly implemented - TODO");
/*        Context ctx = inflater.getContext();

        if(!showMasked)
            addChip(inflater, group, R.id.not_hidden, ctx.getString(R.string.not_hidden_filter));
        if(onlyBlacklisted)
            addChip(inflater, group, R.id.blacklisted, ctx.getString(R.string.malicious_connection_filter));
        if(onlyCleartext)
            addChip(inflater, group, R.id.only_cleartext, ctx.getString(R.string.cleartext_connection));
        if(status != ConnectionDescriptor.Status.STATUS_INVALID) {
            String label = String.format(ctx.getString(R.string.status_filter), ConnectionDescriptor.getStatusLabel(status, ctx));
            addChip(inflater, group, R.id.status_ind, label);
        }
        if(decStatus != ConnectionDescriptor.DecryptionStatus.INVALID) {
            String label = String.format(ctx.getString(R.string.decryption_filter), ConnectionDescriptor.getDecryptionStatusLabel(decStatus, ctx));
            addChip(inflater, group, R.id.decryption_status, label);
        }
        if(filteringStatus != ConnectionDescriptor.FilteringStatus.INVALID) {
            String label = ctx.getString(R.string.firewall_filter, ctx.getString((filteringStatus == FilteringStatus.BLOCKED) ?
                    R.string.blocked_connection_filter : R.string.allowed_connection_filter));
            addChip(inflater, group, R.id.firewall, label);
        }
        if(iface != null)
            addChip(inflater, group, R.id.capture_interface, String.format(ctx.getString(R.string.interface_filter), iface));

        group.setVisibility(group.getChildCount() > 0 ? View.VISIBLE : View.GONE);*/
    }

    // clear one of the filters of toChips
    public void clear(int filter_id) {
        throw new UnsupportedOperationException("addChip method not properly implemented - TODO");
/*        if(filter_id == R.id.not_hidden)
            showMasked = true;
        else if(filter_id == R.id.blacklisted)
            onlyBlacklisted = false;
        else if(filter_id == R.id.only_cleartext)
            onlyCleartext = false;
        else if(filter_id == R.id.status_ind)
            status = ConnectionDescriptor.Status.STATUS_INVALID;
        else if(filter_id == R.id.decryption_status)
            decStatus = ConnectionDescriptor.DecryptionStatus.INVALID;
        else if(filter_id == R.id.firewall)
            filteringStatus = ConnectionDescriptor.FilteringStatus.INVALID;
        else if(filter_id == R.id.capture_interface)
            iface = null;*/
    }

    public void clear() {
        showMasked = true;
        onlyBlacklisted = false;
        onlyCleartext = false;
        status = ConnectionDescriptor.Status.STATUS_INVALID;
        decStatus = ConnectionDescriptor.DecryptionStatus.INVALID;
        filteringStatus = ConnectionDescriptor.FilteringStatus.INVALID;
        iface = null;
        minSize = 0;
    }
}
