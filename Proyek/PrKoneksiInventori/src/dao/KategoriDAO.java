package dao;

import koneksi.KoneksiDB;
import model.Kategori;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Akses data tabel kategori.
 */
public final class KategoriDAO {

    private static final String SQL_SEMUA =
            "SELECT id_kategori, nama_kategori FROM kategori ORDER BY nama_kategori";

    public List<Kategori> semua() throws SQLException {
        List<Kategori> hasil = new ArrayList<Kategori>();
        try (Connection con = KoneksiDB.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_SEMUA);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                hasil.add(new Kategori(rs.getInt("id_kategori"), rs.getString("nama_kategori")));
            }
        }
        return hasil;
    }
}