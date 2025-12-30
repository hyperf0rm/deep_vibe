package io.github.hyperf0rm.deep_vibe.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tracks",
       uniqueConstraints = { @UniqueConstraint(columnNames = {"name", "artistName"}) })
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String artistName;
    private String preview_url;
    private boolean isAnalyzed = false;
    private short bpm;
    private float rms;
    // private String key;
    // private ... spectralCentroid;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getPreview_url() {
        return preview_url;
    }

    public void setPreview_url(String preview_url) {
        this.preview_url = preview_url;
    }

    public boolean isAnalyzed() {
        return isAnalyzed;
    }

    public void setAnalyzed(boolean analyzed) {
        isAnalyzed = analyzed;
    }

    public short getBpm() {
        return bpm;
    }

    public void setBpm(short bpm) {
        this.bpm = bpm;
    }

    public float getRms() {
        return rms;
    }

    public void setRms(float rms) {
        this.rms = rms;
    }

    @Override
    public String toString() {
        return this.artistName + " - " + this.name;
    }
}
