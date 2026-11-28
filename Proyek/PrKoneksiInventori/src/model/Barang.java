package model;

import java.io.Serializable;

/**
 * Satu baris tabel barang beserta nama kategorinya.
 * Objek ini hanya membawa data; aturan validasi ada di form, aturan tabel ada di database.
 */
public final class Barang implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String kode;
    private final String nama;
    private final int idKategori;
    private final String namaKategori;
    private final int jumlahTersedia;
    private final String keterangan;

    public Barang(String kode, String nama, int idKategori, String namaKategori,
            int jumlahTersedia, String keterangan) {
        this.kode = kode;
        this.nama = nama;
        this.idKategori = idKategori;
        this.namaKategori = namaKategori;
        this.jumlahTersedia = jumlahTersedia;
        this.keterangan = keterangan == null ? "" : keterangan;
    }

    public String getKode() {
        return kode;
    }

    public String getNama() {
        return nama;
    }

    public int getIdKategori() {
        return idKategori;
    }

    public String getNamaKategori() {
        return namaKategori;
    }

    public int getJumlahTersedia() {
        return jumlahTersedia;
    }

    public String getKeterangan() {
        return keterangan;
    }
}