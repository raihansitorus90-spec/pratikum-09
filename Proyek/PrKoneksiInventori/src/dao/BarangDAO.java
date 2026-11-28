package dao;

import koneksi.KoneksiDB;
import model.Barang;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Akses data tabel barang: cari (READ), tambah (CREATE), ubah (UPDATE), hapus (DELETE).
 * Semua nilai dari pengguna dikirim melalui parameter ? pada PreparedStatement.
 */
public final class BarangDAO {

    private static final String SQL_CARI =
            "SELECT b.kode_barang, b.nama_barang, b.id_kategori, k.nama_kategori, "
            + "b.jumlah_tersedia, b.keterangan "
            + "FROM barang b LEFT JOIN kategori k ON b.id_kategori = k.id_kategori "
            + "WHERE b.kode_barang LIKE ? ESCAPE '!' "
            + "OR b.nama_barang LIKE ? ESCAPE '!' "
            + "OR k.nama_kategori LIKE ? ESCAPE '!' "
            + "ORDER BY b.kode_barang";

    private static final String SQL_TAMBAH =
            "INSERT INTO barang "
            + "(kode_barang, nama_barang, id_kategori, jumlah_tersedia, keterangan) "
            + "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UBAH =
            "UPDATE barang SET nama_barang = ?, id_kategori = ?, "
            + "jumlah_tersedia = ?, keterangan = ? "
            + "WHERE kode_barang = ?";

    private static final String SQL_ADA =
            "SELECT 1 FROM barang WHERE kode_barang = ?";

    private static final String SQL_HAPUS =
            "DELETE FROM barang WHERE kode_barang = ?";

    /**
     * Mencari barang berdasarkan kode, nama, atau nama kategori.
     * Kata kunci kosong berarti menampilkan semua barang.
     */
    public List<Barang> cari(String kataKunci) throws SQLException {
        String pola = "%" + amankanLike(kataKunci == null ? "" : kataKunci.trim()) + "%";
        List<Barang> hasil = new ArrayList<Barang>();
        try (Connection con = KoneksiDB.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_CARI)) {
            ps.setString(1, pola);
            ps.setString(2, pola);
            ps.setString(3, pola);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasil.add(new Barang(
                            rs.getString("kode_barang"),
                            rs.getString("nama_barang"),
                            rs.getInt("id_kategori"),
                            rs.getString("nama_kategori"),
                            rs.getInt("jumlah_tersedia"),
                            rs.getString("keterangan")));
                }
            }
        }
        return hasil;
    }

    /** Menambah satu barang. Mengembalikan jumlah baris yang tersimpan (normalnya 1). */
    public int tambah(Barang b) throws SQLException {
        try (Connection con = KoneksiDB.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_TAMBAH)) {
            ps.setString(1, b.getKode());
            ps.setString(2, b.getNama());
            ps.setInt(3, b.getIdKategori());
            ps.setInt(4, b.getJumlahTersedia());
            ps.setString(5, b.getKeterangan());
            return ps.executeUpdate();
        }
    }

    /**
     * Mengubah nama, kategori, jumlah, dan keterangan barang dengan kode tertentu.
     * Mengembalikan 1 jika barang ditemukan, 0 jika barang sudah tidak ada.
     */
    public int ubah(Barang b) throws SQLException {
        try (Connection con = KoneksiDB.getConnection()) {
            int baris;
            try (PreparedStatement ps = con.prepareStatement(SQL_UBAH)) {
                ps.setString(1, b.getNama());
                ps.setInt(2, b.getIdKategori());
                ps.setInt(3, b.getJumlahTersedia());
                ps.setString(4, b.getKeterangan());
                ps.setString(5, b.getKode());
                baris = ps.executeUpdate();
            }
            if (baris > 0) {
                return 1;
            }
            // Sebagian pengaturan driver menghitung 0 jika nilai tidak berubah.
            // Karena itu keberadaan barang diperiksa sekali lagi.
            try (PreparedStatement ps = con.prepareStatement(SQL_ADA)) {
                ps.setString(1, b.getKode());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? 1 : 0;
                }
            }
        }
    }

    /** Menghapus barang. Mengembalikan 1 jika terhapus, 0 jika barang sudah tidak ada. */
    public int hapus(String kode) throws SQLException {
        try (Connection con = KoneksiDB.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_HAPUS)) {
            ps.setString(1, kode);
            return ps.executeUpdate();
        }
    }

    /** Membuat karakter % dan _ dibaca sebagai huruf biasa, bukan pola LIKE. */
    static String amankanLike(String teks) {
        return teks.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}