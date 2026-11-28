package model;

import java.io.Serializable;

/**
 * Satu baris tabel kategori.
 * toString() mengembalikan nama, sehingga JComboBox menampilkan nama kategori.
 */
public final class Kategori implements Serializable {

    private static final long serialVersionUID = 1L;
    private final int id;
    private final String nama;

    public Kategori(int id, String nama) {
        this.id = id;
        this.nama = nama;
    }

    public int getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    @Override
    public String toString() {
        return nama;
    }
}