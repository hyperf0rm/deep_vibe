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
    private String previewUrl;
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

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public boolean isAnalyzed() {
        return isAnalyzed;
    }

    public void setAnalyzed(boolean isAnalyzed) {
        this.isAnalyzed = isAnalyzed;
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
