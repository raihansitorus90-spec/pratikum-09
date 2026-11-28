package tampilan;

import akun.Sesi;
import dao.BarangDAO;
import dao.KategoriDAO;
import model.Barang;
import model.Kategori;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class FormBarang extends JFrame {
    private static final long serialVersionUID = 1L;

    // ---------- Komponen pencarian ----------
    private final JTextField txtCari = new JTextField(22);
    private final JButton btnCari = new JButton("Cari");
    private final JButton btnSemua = new JButton("Tampilkan Semua");

    // ---------- Komponen form ----------
    private final JTextField txtKode = new JTextField(20);
    private final JTextField txtNama = new JTextField(20);
    private final JComboBox<Kategori> cmbKategori = new JComboBox<Kategori>();
    private final JTextField txtJumlah = new JTextField("0", 20);
    private final JTextField txtKeterangan = new JTextField(20);
    private final JLabel lblMode = new JLabel();
    private final JButton btnTambah = new JButton("Tambah");
    private final JButton btnUbah = new JButton("Ubah");
    private final JButton btnHapus = new JButton("Hapus");
    private final JButton btnBersihkan = new JButton("Bersihkan Form");
    private final JButton btnMuat = new JButton("Muat Ulang Data");
    private final JButton btnKeluar = new JButton("Keluar Akun");
    private final JLabel lblStatus = new JLabel("Menyiapkan aplikasi...");

    // ---------- Tabel ----------
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Kode", "Nama Barang", "Kategori", "Jumlah", "Keterangan"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 3 ? Integer.class : String.class;
        }
    };
    private final JTable tabel = new JTable(model);

    // ---------- Keadaan form ----------
    private final ArrayList<Barang> daftarBarang = new ArrayList<Barang>();
    private final Sesi sesi;
    private boolean sibuk = false;
    /** null = mode data baru; berisi kode = mode mengubah barang tersebut. */
    private String kodeTerpilih = null;
    private String kataKunci = "";

    /** Pekerjaan database yang mengubah data dan mengembalikan jumlah baris. */
    private interface Perubahan {
        int jalankan() throws SQLException;
    }

    /** Hasil pembacaan kategori dan barang dari database. */
    private static class DataTampilan {
        private List<Kategori> kategori;
        private List<Barang> barang;
    }

    public FormBarang(Sesi sesi) {
        super("Inventori Laboratorium | P10 - Ubah, Hapus, dan Cari");
        this.sesi = Objects.requireNonNull(sesi, "FormBarang hanya dapat dibuka setelah login.");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (sibuk) {
                    JOptionPane.showMessageDialog(FormBarang.this,
                            "Tunggu sampai proses database selesai.");
                } else {
                    dispose();
                }
            }
        });
        susunTampilan();
        pasangAksi();
        pack();
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);
        muatData("");
    }

    // =====================================================================
    // TAMPILAN
    // =====================================================================

    private void susunTampilan() {
        JPanel utama = new JPanel(new BorderLayout(12, 12));
        utama.setBorder(new EmptyBorder(18, 18, 18, 18));
        utama.setBackground(new Color(238, 243, 249));
        setContentPane(utama);

        JLabel judul = new JLabel("INVENTORI LABORATORIUM");
        judul.setFont(new Font("SansSerif", Font.BOLD, 24));
        judul.setForeground(new Color(24, 49, 83));
        JLabel lblPengguna = new JLabel("Masuk sebagai: " + sesi.getNama()
                + " (" + sesi.getUsername() + ") | Peran: " + sesi.getPeran().toUpperCase());
        lblPengguna.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel lblHak = new JLabel(sesi.isAdmin()
                ? "Hak akses: menambah, mengubah, menghapus, mencari, dan melihat data."
                : "Hak akses: mencari dan melihat data (mode baca).");
        lblHak.setForeground(sesi.isAdmin() ? new Color(20, 110, 50) : new Color(170, 80, 0));
        JPanel infoSesi = new JPanel(new BorderLayout(0, 2));
        infoSesi.setOpaque(false);
        infoSesi.add(lblPengguna, BorderLayout.NORTH);
        infoSesi.add(lblHak, BorderLayout.SOUTH);
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(judul, BorderLayout.NORTH);
        header.add(infoSesi, BorderLayout.SOUTH);
        utama.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(sesi.isAdmin()
                ? "Data Barang" : "Data Barang (hanya dapat dilihat)"));
        tambahBaris(form, 0, "Kode barang", txtKode);
        tambahBaris(form, 1, "Nama barang", txtNama);
        tambahBaris(form, 2, "Kategori", cmbKategori);
        tambahBaris(form, 3, "Jumlah tersedia", txtJumlah);
        tambahBaris(form, 4, "Keterangan (boleh kosong)", txtKeterangan);
        lblMode.setFont(new Font("SansSerif", Font.BOLD, 12));
        GridBagConstraints gm = new GridBagConstraints();
        gm.gridx = 0;
        gm.gridy = 5;
        gm.gridwidth = 2;
        gm.anchor = GridBagConstraints.WEST;
        gm.insets = new Insets(4, 8, 0, 8);
        form.add(lblMode, gm);

        btnTambah.setToolTipText("Simpan barang baru (Ctrl+S). Khusus admin.");
        btnUbah.setToolTipText("Simpan perubahan barang yang dipilih. Khusus admin.");
        btnHapus.setToolTipText("Hapus barang yang dipilih (tombol Delete pada tabel). Khusus admin.");
        btnBersihkan.setToolTipText("Kosongkan form dan kembali ke mode data baru");
        btnKeluar.setToolTipText("Tutup sesi ini dan kembali ke jendela login");
        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        tombol.add(btnTambah);
        tombol.add(btnUbah);
        tombol.add(btnHapus);
        tombol.add(btnBersihkan);
        tombol.add(btnMuat);
        tombol.add(btnKeluar);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 6;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 6, 8, 6);
        form.add(tombol, g);

        JPanel pnlCari = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlCari.setOpaque(false);
        pnlCari.add(new JLabel("Cari (kode / nama / kategori):"));
        pnlCari.add(txtCari);
        pnlCari.add(btnCari);
        pnlCari.add(btnSemua);

        tabel.setRowHeight(26);
        tabel.setAutoCreateRowSorter(true);
        tabel.setFillsViewportHeight(true);
        tabel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabel.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(tabel);
        scroll.setPreferredSize(new Dimension(860, 230));
        scroll.setBorder(BorderFactory.createTitledBorder(
                "Daftar Barang - klik satu baris untuk memilih"));

        JPanel atas = new JPanel(new BorderLayout(0, 8));
        atas.setOpaque(false);
        atas.add(form, BorderLayout.NORTH);
        atas.add(pnlCari, BorderLayout.SOUTH);
        JPanel tengah = new JPanel(new BorderLayout(10, 10));
        tengah.setOpaque(false);
        tengah.add(atas, BorderLayout.NORTH);
        tengah.add(scroll, BorderLayout.CENTER);
        utama.add(tengah, BorderLayout.CENTER);
        utama.add(lblStatus, BorderLayout.SOUTH);
    }

    private void tambahBaris(JPanel panel, int baris, String teks, JComponent isi) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = baris;
        g.insets = new Insets(5, 8, 5, 8);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        panel.add(new JLabel(teks), g);
        g.gridx = 1;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(isi, g);
    }

    // =====================================================================
    // AKSI TOMBOL DAN KEYBOARD
    // =====================================================================

    private void pasangAksi() {
        btnTambah.addActionListener(e -> tambahBarang());
        btnUbah.addActionListener(e -> ubahBarang());
        btnHapus.addActionListener(e -> hapusBarang());
        btnBersihkan.addActionListener(e -> bersihkanForm());
        btnMuat.addActionListener(e -> muatData(""));
        btnKeluar.addActionListener(e -> keluarAkun());
        btnCari.addActionListener(e -> cari());
        txtCari.addActionListener(e -> cari());
        btnSemua.addActionListener(e -> {
            txtCari.setText("");
            cari();
        });

        // Klik satu baris tabel -> isi form dengan data baris tersebut.
        tabel.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                pilihBaris();
            }
        });

        // Jalur lain menuju tindakan: Ctrl+S untuk Tambah, tombol Delete pada tabel untuk Hapus.
        // Jalur ini tetap aktif walaupun tombolnya abu-abu, sehingga pemeriksaan
        // hak akses di dalam tambahBarang() dan hapusBarang() benar-benar diuji.
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "tambahBarang");
        root.getActionMap().put("tambahBarang", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                tambahBarang();
            }
        });
        tabel.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "hapusBarang");
        tabel.getActionMap().put("hapusBarang", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                hapusBarang();
            }
        });
        aturSibuk(false);
    }

    private void aturSibuk(boolean nilai) {
        sibuk = nilai;
        aturTombol();
    }

    /** Mengatur komponen yang aktif berdasarkan: sedang sibuk, peran, dan mode form. */
    private void aturTombol() {
        boolean siap = !sibuk;
        boolean admin = sesi.isAdmin();
        boolean modeBaru = kodeTerpilih == null;
        boolean adaKategori = cmbKategori.getItemCount() > 0;

        txtKode.setEnabled(siap && admin && modeBaru);
        txtNama.setEnabled(siap && admin);
        cmbKategori.setEnabled(siap && admin);
        txtJumlah.setEnabled(siap && admin);
        txtKeterangan.setEnabled(siap && admin);

        btnTambah.setEnabled(siap && admin && modeBaru && adaKategori);
        btnUbah.setEnabled(siap && admin && !modeBaru && adaKategori);
        btnHapus.setEnabled(siap && admin && !modeBaru);
        btnBersihkan.setEnabled(siap);
        btnMuat.setEnabled(siap);
        btnKeluar.setEnabled(siap);
        txtCari.setEnabled(siap);
        btnCari.setEnabled(siap);
        btnSemua.setEnabled(siap);
        tabel.setEnabled(siap);

        lblMode.setText(modeBaru
                ? "Mode: data baru. Isi form lalu klik Tambah, atau klik baris tabel untuk memilih."
                : "Mode: barang " + kodeTerpilih + " dipilih. Kode tidak dapat diubah.");
        lblMode.setForeground(modeBaru ? new Color(60, 60, 60) : new Color(20, 70, 160));
    }

    // =====================================================================
    // FORM
    // =====================================================================

    private void pilihBaris() {
        int barisTampil = tabel.getSelectedRow();
        if (barisTampil < 0) {
            return;
        }
        // Tabel dapat diurutkan dengan klik judul kolom. Nomor baris tampilan
        // harus diubah ke nomor baris model sebelum mengambil data.
        int barisModel = tabel.convertRowIndexToModel(barisTampil);
        if (barisModel < 0 || barisModel >= daftarBarang.size()) {
            return;
        }
        Barang b = daftarBarang.get(barisModel);
        kodeTerpilih = b.getKode();
        txtKode.setText(b.getKode());
        txtNama.setText(b.getNama());
        pilihKategori(b.getIdKategori());
        txtJumlah.setText(String.valueOf(b.getJumlahTersedia()));
        txtKeterangan.setText(b.getKeterangan());
        lblStatus.setText("Barang " + b.getKode() + " dipilih.");
        aturTombol();
    }

    private void pilihKategori(int idKategori) {
        cmbKategori.setSelectedIndex(-1);
        for (int i = 0; i < cmbKategori.getItemCount(); i++) {
            if (cmbKategori.getItemAt(i).getId() == idKategori) {
                cmbKategori.setSelectedIndex(i);
                return;
            }
        }
    }

    private void bersihkanForm() {
        kodeTerpilih = null;
        tabel.clearSelection();
        txtKode.setText("");
        txtNama.setText("");
        txtJumlah.setText("0");
        txtKeterangan.setText("");
        if (cmbKategori.getItemCount() > 0) {
            cmbKategori.setSelectedIndex(0);
        }
        aturTombol();
        txtKode.requestFocusInWindow();
    }

    /**
     * Membaca dan memeriksa isian form.
     * Mengembalikan objek Barang jika benar, atau null jika ada isian yang salah.
     */
    private Barang bacaForm(String kode) {
        String nama = txtNama.getText().trim();
        String ket = txtKeterangan.getText().trim();
        Kategori kategori = (Kategori) cmbKategori.getSelectedItem();
        if (kode.isEmpty() || nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kode dan nama barang wajib diisi.");
            return null;
        }
        if (kode.length() > 20 || nama.length() > 100 || ket.length() > 255) {
            JOptionPane.showMessageDialog(this,
                    "Batas karakter: kode 20, nama 100, keterangan 255.");
            return null;
        }
        if (kategori == null) {
            JOptionPane.showMessageDialog(this, "Pilih kategori. Jika kosong, klik Muat Ulang Data.");
            return null;
        }
        int jumlah;
        try {
            jumlah = Integer.parseInt(txtJumlah.getText().trim());
            if (jumlah < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Jumlah harus bilangan bulat 0 sampai 2147483647, tanpa titik atau koma.");
            return null;
        }
        return new Barang(kode, nama, kategori.getId(), kategori.getNama(), jumlah, ket);
    }

    // =====================================================================
    // CREATE, UPDATE, DELETE (dengan pemeriksaan hak akses)
    // =====================================================================

    private void tambahBarang() {
        if (!sesi.isAdmin()) {
            tolakAkses("menambah");
            return;
        }
        if (sibuk) {
            return;
        }
        if (kodeTerpilih != null) {
            JOptionPane.showMessageDialog(this, "Anda sedang memilih barang " + kodeTerpilih
                    + ".\nKlik Bersihkan Form terlebih dahulu untuk menambah barang baru.");
            return;
        }
        final Barang b = bacaForm(txtKode.getText().trim());
        if (b == null) {
            return;
        }
        jalankanPerubahan("Menyimpan barang baru...",
                "Barang " + b.getKode() + " berhasil ditambahkan.",
                () -> new BarangDAO().tambah(b));
    }

    private void ubahBarang() {
        if (!sesi.isAdmin()) {
            tolakAkses("mengubah");
            return;
        }
        if (sibuk) {
            return;
        }
        if (kodeTerpilih == null) {
            JOptionPane.showMessageDialog(this, "Klik dahulu barang yang akan diubah pada tabel.");
            return;
        }
        final Barang b = bacaForm(kodeTerpilih);
        if (b == null) {
            return;
        }
        jalankanPerubahan("Menyimpan perubahan...",
                "Perubahan barang " + b.getKode() + " berhasil disimpan.",
                () -> new BarangDAO().ubah(b));
    }

    private void hapusBarang() {
        if (!sesi.isAdmin()) {
            tolakAkses("menghapus");
            return;
        }
        if (sibuk) {
            return;
        }
        if (kodeTerpilih == null) {
            JOptionPane.showMessageDialog(this, "Klik dahulu barang yang akan dihapus pada tabel.");
            return;
        }
        final String kode = kodeTerpilih;
        int jawab = JOptionPane.showConfirmDialog(this,
                "Hapus barang " + kode + " - " + txtNama.getText().trim() + "?\n"
                + "Data yang sudah dihapus tidak dapat dikembalikan.",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (jawab != JOptionPane.YES_OPTION) {
            lblStatus.setText("Penghapusan dibatalkan.");
            return;
        }
        jalankanPerubahan("Menghapus barang...",
                "Barang " + kode + " berhasil dihapus.",
                () -> new BarangDAO().hapus(kode));
    }

    private void tolakAkses(String tindakan) {
        lblStatus.setText("Ditolak: peran " + sesi.getPeran() + " tidak boleh "
                + tindakan + " barang.");
        JOptionPane.showMessageDialog(this,
                "Akses ditolak. Hanya admin yang boleh " + tindakan + " barang.",
                "Hak Akses", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Menjalankan tambah/ubah/hapus di latar belakang, lalu memuat ulang daftar.
     */
    private void jalankanPerubahan(final String pesanProses, final String pesanBerhasil,
            final Perubahan perubahan) {
        aturSibuk(true);
        lblStatus.setText(pesanProses);
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws SQLException {
                return perubahan.jalankan();
            }

            @Override
            protected void done() {
                boolean muatUlang = false;
                String pesanMuat = "";
                try {
                    int baris = get();
                    if (baris == 1) {
                        muatUlang = true;
                        pesanMuat = pesanBerhasil + " ";
                        JOptionPane.showMessageDialog(FormBarang.this, pesanBerhasil);
                    } else if (baris == 0) {
                        muatUlang = true;
                        pesanMuat = "Data tidak ditemukan. ";
                        JOptionPane.showMessageDialog(FormBarang.this,
                                "Barang tidak ditemukan di database.\n"
                                + "Kemungkinan sudah dihapus oleh pengguna lain.\n"
                                + "Daftar akan dimuat ulang.",
                                "Data Berubah", JOptionPane.WARNING_MESSAGE);
                    } else {
                        lblStatus.setText("Jumlah baris terdampak tidak wajar: " + baris);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblStatus.setText("Proses terhenti. Periksa data sebelum mengulang.");
                } catch (ExecutionException e) {
                    lblStatus.setText("Proses gagal. Data tidak berubah.");
                    tampilkanError("Perubahan data tidak berhasil.", e.getCause());
                } finally {
                    aturSibuk(false);
                }
                if (muatUlang) {
                    bersihkanForm();
                    muatData(pesanMuat);
                }
            }
        }.execute();
    }

    private void keluarAkun() {
        if (sibuk) {
            JOptionPane.showMessageDialog(this, "Tunggu sampai proses database selesai.");
            return;
        }
        dispose();
        new FormLogin().setVisible(true);
    }

    // =====================================================================
    // READ: memuat dan mencari data
    // =====================================================================

    private void cari() {
        if (sibuk) {
            return;
        }
        String teks = txtCari.getText().trim();
        if (teks.length() > 50) {
            JOptionPane.showMessageDialog(this, "Kata kunci maksimal 50 karakter.");
            return;
        }
        kataKunci = teks;
        bersihkanForm();
        muatData("");
    }

    private void muatData(final String pesanAwal) {
        if (sibuk) {
            return;
        }
        final String kunci = kataKunci;
        Kategori pilihanLama = (Kategori) cmbKategori.getSelectedItem();
        final Integer idTerpilih = pilihanLama == null ? null : pilihanLama.getId();
        aturSibuk(true);
        lblStatus.setText(pesanAwal + "Memuat data dari database...");
        new SwingWorker<DataTampilan, Void>() {
            @Override
            protected DataTampilan doInBackground() throws SQLException {
                DataTampilan data = new DataTampilan();
                data.kategori = new KategoriDAO().semua();
                data.barang = new BarangDAO().cari(kunci);
                return data;
            }

            @Override
            protected void done() {
                try {
                    DataTampilan data = get();
                    cmbKategori.removeAllItems();
                    for (Kategori k : data.kategori) {
                        cmbKategori.addItem(k);
                    }
                    if (idTerpilih != null) {
                        pilihKategori(idTerpilih);
                    }
                    if (cmbKategori.getSelectedIndex() < 0 && cmbKategori.getItemCount() > 0) {
                        cmbKategori.setSelectedIndex(0);
                    }
                    daftarBarang.clear();
                    daftarBarang.addAll(data.barang);
                    model.setRowCount(0);
                    for (Barang b : daftarBarang) {
                        model.addRow(new Object[]{
                            b.getKode(), b.getNama(), b.getNamaKategori(),
                            b.getJumlahTersedia(), b.getKeterangan()
                        });
                    }
                    String status;
                    if (kunci.isEmpty()) {
                        status = pesanAwal + "Data terbaru: " + model.getRowCount() + " barang.";
                    } else {
                        status = pesanAwal + "Hasil pencarian \"" + kunci + "\": "
                                + model.getRowCount() + " barang.";
                    }
                    if (data.kategori.isEmpty()) {
                        status += " Kategori kosong: laporkan kepada dosen.";
                    }
                    lblStatus.setText(status);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lblStatus.setText("Pemuatan terhenti. Klik Muat Ulang Data.");
                } catch (ExecutionException e) {
                    lblStatus.setText(pesanAwal + "Gagal memuat. Tabel belum diperbarui; klik Muat Ulang Data.");
                    tampilkanError(pesanAwal + "Pembacaan data gagal.", e.getCause());
                } finally {
                    aturSibuk(false);
                }
            }
        }.execute();
    }

    // =====================================================================
    // PESAN KESALAHAN DATABASE
    // =====================================================================

    private void tampilkanError(String konteks, Throwable sebab) {
        String pesan = konteks;
        if (sebab instanceof SQLException) {
            SQLException e = (SQLException) sebab;
            int kode = e.getErrorCode();
            if (kode == 1062) {
                pesan += "\nKode barang sudah digunakan. Gunakan kode lain.";
            } else if (kode == 1451) {
                pesan += "\nBarang ini sudah tercatat pada data peminjaman,"
                        + "\nsehingga tidak boleh dihapus agar riwayat tetap utuh.";
            } else if (kode == 1142) {
                pesan += "\nAkun database belum memiliki hak yang diperlukan."
                        + "\nJalankan GRANT UPDATE, DELETE pada Langkah 4 modul.";
            } else if (kode == 1452) {
                pesan += "\nKategori tidak ditemukan. Klik Muat Ulang Data, lalu pilih kategori lagi.";
            } else if (kode == 1406) {
                pesan += "\nIsian terlalu panjang untuk kolom database.";
            } else if (kode == 4025 || kode == 3819) {
                pesan += "\nNilai melanggar aturan database (CHECK).";
            } else {
                pesan += "\nPeriksa MySQL, konfigurasi koneksi, driver, dan struktur tabel.";
            }
            pesan += "\nSQLState: " + e.getSQLState() + "; kode: " + kode;
        } else if (sebab != null) {
            pesan += "\nJenis gangguan: " + sebab.getClass().getSimpleName();
        }
        JOptionPane.showMessageDialog(this, pesan, "Informasi Database", JOptionPane.ERROR_MESSAGE);
    }

    /** Menjalankan FormBarang langsung akan membuka login terlebih dahulu. */
    public static void main(String[] args) {
        FormLogin.main(args);
    }
}