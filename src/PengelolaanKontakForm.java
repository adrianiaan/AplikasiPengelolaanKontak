import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author ADRIAN WIN
 */
public class PengelolaanKontakForm extends javax.swing.JFrame {

    /**
     * Creates new form PengelolaanKontakForm
     */
    private DefaultTableModel model; // Deklarasikan model di sini
    
    private void kosongkanInputKolom() {
        txtNama.setText("");
        txtNomorTelpon.setText("");
        txtCari.setText(""); // Kosongkan field pencarian
        cmbKategori.setSelectedIndex(0); // Pilih item pertama di ComboBox
    }


    public PengelolaanKontakForm() {
        initComponents();
        buatDatabase(); // Membuat database dan tabel kontak jika belum ada

        // Nonaktifkan tombol Edit dan Hapus di awal
        btnEdit.setEnabled(false);
        btnHapus.setEnabled(false);
        
        // Cek koneksi database saat aplikasi dimulai
        if (connect() == null) {
            JOptionPane.showMessageDialog(this, "Gagal terhubung ke database. Aplikasi akan ditutup.");
            System.exit(0); // Keluar dari aplikasi jika koneksi gagal
        }

        // Tambahkan item ke ComboBox kategori
        cmbKategori.addItem("Keluarga");
        cmbKategori.addItem("Teman");
        cmbKategori.addItem("Rekan Kerja");
        
        // Inisialisasi model tabel
        model = new DefaultTableModel(new String[]{"ID", "Nama", "Nomor Telepon", "Kategori"}, 0);
        tblDaftarKontak.setModel(model);
        
        // Sembunyikan kolom ID
        tblDaftarKontak.getColumnModel().getColumn(0).setMinWidth(0);
        tblDaftarKontak.getColumnModel().getColumn(0).setMaxWidth(0);
        tblDaftarKontak.getColumnModel().getColumn(0).setWidth(0);
        
        // Kosongkan teks awal pada kolom input
        txtNama.setText("Masukkan Nama");
        txtNomorTelpon.setText("Masukkan Nomor Telepon");

        // Menambahkan FocusListener untuk txtNama dan txtNomorTelpon
        txtNama.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtNamaFocusGained(evt);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtNamaFocusLost(evt);
            }
        });

        txtNomorTelpon.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtNomorTelponFocusGained(evt);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtNomorTelponFocusLost(evt);
            }
        });
        
        // Tambahkan KeyListener ke txtNomorTelpon untuk menerima angka saja
        txtNomorTelpon.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                // Periksa jika karakter bukan angka atau bukan backspace
                if (!Character.isDigit(c) && c != '\b') {
                    evt.consume(); // Abaikan karakter yang tidak valid
                    JOptionPane.showMessageDialog(null, "Hanya boleh memasukkan angka!", "Input Tidak Valid", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        
        //Menambahkan Placeholder di txtCari
        txtCari.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtCari.getText().equals("Cari Kontak")) {
                    txtCari.setText(""); // Hapus placeholder
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtCari.getText().isEmpty()) {
                    txtCari.setText("Cari Kontak"); // Kembalikan placeholder jika kosong
                }
            }
        });

    
        // Muat data kontak ke dalam tabel
        tampilkanKontak();
        
        // Tambahkan ActionListener untuk tombol CRUD
        setupActionListeners();

        // Tambahkan event listener untuk mendeteksi klik pada baris tabel
        tblDaftarKontak.getSelectionModel().addListSelectionListener(event -> {
            int selectedRow = tblDaftarKontak.getSelectedRow();
            // Aktifkan atau nonaktifkan tombol berdasarkan seleksi tabel
            btnEdit.setEnabled(selectedRow >= 0);
            btnHapus.setEnabled(selectedRow >= 0);
            if (selectedRow >= 0) {
                // Ambil data dari tabel dan isi ke kolom input
                txtNama.setText(model.getValueAt(selectedRow, 1).toString());
                txtNomorTelpon.setText(model.getValueAt(selectedRow, 2).toString());
                cmbKategori.setSelectedItem(model.getValueAt(selectedRow, 3).toString());
            }
        });
    }

    private void setupActionListeners() {
        // Tombol Tambah
        btnTambah.addActionListener(e -> {
            String nama = txtNama.getText();
            String nomorTelepon = txtNomorTelpon.getText();
            String kategori = cmbKategori.getSelectedItem().toString();

            // Validasi input
            if (nama.isEmpty() || nomorTelepon.isEmpty() || nama.equals("Masukkan Nama") || nomorTelepon.equals("Masukkan Nomor Telepon")) {
                JOptionPane.showMessageDialog(this, "Nama dan Nomor Telepon tidak boleh kosong.");
                return;
            }
            
            // Validasi nomor telepon hanya angka
            if (!nomorTelepon.matches("\\d+")) {
                JOptionPane.showMessageDialog(this, "Nomor telepon hanya boleh berisi angka.");
                return;
            }
            
            tambahKontak(nama, nomorTelepon, kategori);
            tampilkanKontak(); // Refresh tabel setelah menambah kontak
            kosongkanInputKolom();
        });

        // Tombol Edit
        btnEdit.addActionListener(e -> {
            int selectedRow = tblDaftarKontak.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) model.getValueAt(selectedRow, 0); // Ambil ID dari kolom pertama
                String nama = txtNama.getText();
                String nomorTelepon = txtNomorTelpon.getText();
                String kategori = cmbKategori.getSelectedItem().toString();
                editKontak(id, nama, nomorTelepon, kategori);
                tampilkanKontak(); // Refresh tabel setelah mengedit kontak
                kosongkanInputKolom();
            } else {
                JOptionPane.showMessageDialog(this, "Pilih kontak yang ingin diedit.");
            }
        });

        // Tombol Hapus
        btnHapus.addActionListener(e -> {
            int selectedRow = tblDaftarKontak.getSelectedRow();
            if (selectedRow >= 0) {
                int id = (int) model.getValueAt(selectedRow, 0); // Ambil ID dari kolom pertama

                int confirm = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin menghapus kontak ini?",
                        "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    hapusKontak(id);
                    tampilkanKontak(); // Refresh tabel setelah menghapus kontak
                }
            } else {
                JOptionPane.showMessageDialog(this, "Pilih kontak yang ingin dihapus.");
            }
        });

        // Tombol Cari
        btnCari.addActionListener(e -> {
            String keyword = txtCari.getText().trim(); // Ambil input pencarian dan hilangkan spasi
            if (keyword.isEmpty() || keyword.equals("Masukkan Kata Kunci")) {
                JOptionPane.showMessageDialog(this, "Masukkan kata kunci untuk pencarian.");
                return;
            }
            cariKontak(keyword); // Lakukan pencarian
            kosongkanInputKolom(); // Kosongkan field pencarian setelah selesai
            txtCari.setText("Cari Kontak"); // Reset placeholder
        });
        
        // Tombol Import (Muat Data Kontak dari File .CSV)
        btnImport.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // Validasi file harus berformat CSV
                if (!file.getName().endsWith(".csv")) {
                    JOptionPane.showMessageDialog(this, "Harap pilih file dengan format CSV.");
                    return;
                }

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line = br.readLine(); // Baca header
                    if (line == null || !line.equals("Nama,Nomor Telepon,Kategori")) {
                        JOptionPane.showMessageDialog(this, "Format file CSV tidak valid. Harus berisi header: Nama,Nomor Telepon,Kategori.");
                        return;
                    }

                    // Proses membaca baris berikutnya
                    while ((line = br.readLine()) != null) {
                        String[] data = line.split(","); // Pisahkan data berdasarkan koma
                        if (data.length != 3) {
                            JOptionPane.showMessageDialog(this, "Baris tidak valid: " + line);
                            continue;
                        }

                        String nama = data[0].trim();
                        String nomorTelepon = data[1].trim();
                        String kategori = data[2].trim();

                        // Validasi dan tambahkan ke database
                        if (!nomorTelepon.matches("\\d+")) {
                            JOptionPane.showMessageDialog(this, "Nomor telepon tidak valid: " + nomorTelepon);
                            continue;
                        }
                        tambahKontak(nama, nomorTelepon, kategori);
                    }

                    tampilkanKontak(); // Refresh tabel setelah import
                    JOptionPane.showMessageDialog(this, "Data berhasil diimpor dari file CSV.");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat membaca file: " + ex.getMessage());
                }
            }
        });

        // Tombol Export (Simpan Data Kontak ke File .CSV)
        btnExport.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                // Pastikan file memiliki ekstensi .csv
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getAbsolutePath() + ".csv");
                }

                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                    // Tulis header
                    bw.write("Nama,Nomor Telepon,Kategori");
                    bw.newLine();

                    // Ambil data dari tabel dan tulis ke file
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String nama = model.getValueAt(i, 1).toString();
                        String nomorTelepon = model.getValueAt(i, 2).toString();
                        String kategori = model.getValueAt(i, 3).toString();
                        bw.write(nama + "," + nomorTelepon + "," + kategori);
                        bw.newLine();
                    }

                    JOptionPane.showMessageDialog(this, "Data berhasil diekspor ke file CSV.");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat menulis file: " + ex.getMessage());
                }
            }
        });
    }


    // Metode untuk membuat database dan tabel jika belum ada
    private void buatDatabase() {
        String url = "jdbc:sqlite:kontak.db"; // Nama file database

        // SQL untuk membuat tabel kontak
        String sqlCreateTable = "CREATE TABLE IF NOT EXISTS kontak ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "nama TEXT NOT NULL,"
                + "nomor_telepon TEXT NOT NULL,"
                + "kategori TEXT NOT NULL"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // Membuat database dan tabel
            if (conn != null) {
                stmt.execute(sqlCreateTable);
                System.out.println("Database dan tabel kontak berhasil dibuat atau sudah ada.");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Metode koneksi untuk operasi CRUD
    public Connection connect() {
        String url = "jdbc:sqlite:kontak.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            System.out.println("Koneksi berhasil!");
        } catch (SQLException e) {
            System.out.println("Koneksi gagal: " + e.getMessage());
        }
        return conn;
    }
    
    private void cariKontak(String keyword) {
        model.setRowCount(0); // Hapus semua baris sebelum menampilkan data hasil pencarian
        String sql = "SELECT * FROM kontak WHERE nama LIKE ? OR nomor_telepon LIKE ? OR kategori LIKE ?";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            pstmt.setString(3, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nama"),
                    rs.getString("nomor_telepon"),
                    rs.getString("kategori")
                });
            }

            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Kontak tidak ditemukan.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal mencari kontak: " + e.getMessage());
        }
    }

    
    private void tambahKontak(String nama, String nomorTelepon, String kategori) {
        String sql = "INSERT INTO kontak(nama, nomor_telepon, kategori) VALUES(?,?,?)";
        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nama);
            pstmt.setString(2, nomorTelepon);
            pstmt.setString(3, kategori);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Kontak berhasil ditambahkan.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal menambahkan kontak: " + e.getMessage());
        }
    }
    
    private void tampilkanKontak() {
        model.setRowCount(0); // Hapus semua baris sebelum menampilkan data
        String sql = "SELECT * FROM kontak";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nama"),
                    rs.getString("nomor_telepon"),
                    rs.getString("kategori")
                });
            }
            kosongkanInputKolom(); // Bersihkan input setelah menampilkan data
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal menampilkan kontak: " + e.getMessage());
        }
    }
    
    private void editKontak(int id, String nama, String nomorTelepon, String kategori) {
        String sql = "UPDATE kontak SET nama = ?, nomor_telepon = ?, kategori = ? WHERE id = ?";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nama);
            pstmt.setString(2, nomorTelepon);
            pstmt.setString(3, kategori);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Kontak berhasil diperbarui.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memperbarui kontak: " + e.getMessage());
        }
    }
    
    private void hapusKontak(int id) {
        String sql = "DELETE FROM kontak WHERE id = ?";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Kontak berhasil dihapus.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal menghapus kontak: " + e.getMessage());
        }
    }
    



    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        lblNama = new javax.swing.JLabel();
        lblNomor = new javax.swing.JLabel();
        lblKategori = new javax.swing.JLabel();
        txtNama = new javax.swing.JTextField();
        txtNomorTelpon = new javax.swing.JTextField();
        cmbKategori = new javax.swing.JComboBox<>();
        btnTambah = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnCari = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblDaftarKontak = new javax.swing.JTable();
        txtCari = new javax.swing.JTextField();
        btnImport = new javax.swing.JButton();
        btnExport = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Aplikasi Pengelolaan Kontak");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Aplikasi Pengelolaan Kontak", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.ABOVE_TOP, new java.awt.Font("Roboto", 1, 24), new java.awt.Color(0, 102, 255))); // NOI18N
        jPanel1.setLayout(new java.awt.GridBagLayout());

        lblNama.setFont(new java.awt.Font("Roboto", 1, 12)); // NOI18N
        lblNama.setText("Nama");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(lblNama, gridBagConstraints);

        lblNomor.setFont(new java.awt.Font("Roboto", 1, 12)); // NOI18N
        lblNomor.setText("Nomor Telpon");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(lblNomor, gridBagConstraints);

        lblKategori.setFont(new java.awt.Font("Roboto", 1, 12)); // NOI18N
        lblKategori.setText("Kategori");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(lblKategori, gridBagConstraints);

        txtNama.setFont(new java.awt.Font("Roboto", 0, 12)); // NOI18N
        txtNama.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtNamaFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtNamaFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 142;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(txtNama, gridBagConstraints);

        txtNomorTelpon.setFont(new java.awt.Font("Roboto", 0, 12)); // NOI18N
        txtNomorTelpon.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtNomorTelponFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtNomorTelponFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 142;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(txtNomorTelpon, gridBagConstraints);

        cmbKategori.setFont(new java.awt.Font("Roboto", 0, 12)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 142;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(cmbKategori, gridBagConstraints);

        btnTambah.setFont(new java.awt.Font("Roboto", 1, 12)); // NOI18N
        btnTambah.setText("Tambah Kontak");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(btnTambah, gridBagConstraints);

        btnEdit.setFont(new java.awt.Font("Roboto", 1, 12)); // NOI18N
        btnEdit.setText("Edit Kontak");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(btnEdit, gridBagConstraints);

        btnHapus.setFont(new java.awt.Font("Roboto", 1, 12)); // NOI18N
        btnHapus.setText("Hapus Kontak");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(btnHapus, gridBagConstraints);

        btnCari.setFont(new java.awt.Font("Roboto", 1, 12)); // NOI18N
        btnCari.setText("Cari");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(btnCari, gridBagConstraints);

        tblDaftarKontak.setFont(new java.awt.Font("Roboto", 0, 12)); // NOI18N
        tblDaftarKontak.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblDaftarKontak);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 420;
        gridBagConstraints.ipady = 220;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jScrollPane1, gridBagConstraints);

        txtCari.setFont(new java.awt.Font("Roboto", 0, 12)); // NOI18N
        txtCari.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtCariFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtCariFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 100;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 80);
        jPanel1.add(txtCari, gridBagConstraints);

        btnImport.setFont(new java.awt.Font("Roboto", 1, 12)); // NOI18N
        btnImport.setText("Muat Kontak");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(btnImport, gridBagConstraints);

        btnExport.setFont(new java.awt.Font("Roboto", 1, 12)); // NOI18N
        btnExport.setText("Simpan Kontak");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(btnExport, gridBagConstraints);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtNamaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtNamaFocusGained
        if (txtNama.getText().equals("Masukkan Nama")) {
            txtNama.setText(""); // Hapus placeholder
            txtNama.setForeground(java.awt.Color.BLACK); // Warna teks input
        }
    }//GEN-LAST:event_txtNamaFocusGained

    private void txtNamaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtNamaFocusLost
        if (txtNama.getText().isEmpty()) {
            txtNama.setText("Masukkan Nama"); // Kembalikan placeholder jika kosong
            txtNama.setForeground(java.awt.Color.GRAY); // Warna placeholder
        }
    }//GEN-LAST:event_txtNamaFocusLost

    private void txtNomorTelponFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtNomorTelponFocusGained
        if (txtNomorTelpon.getText().equals("Masukkan Nomor Telepon")) {
            txtNomorTelpon.setText(""); // Hapus placeholder
            txtNomorTelpon.setForeground(java.awt.Color.BLACK); // Warna teks input
        }
    }//GEN-LAST:event_txtNomorTelponFocusGained

    private void txtNomorTelponFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtNomorTelponFocusLost
        if (txtNomorTelpon.getText().isEmpty()) {
            txtNomorTelpon.setText("Masukkan Nomor Telepon"); // Kembalikan placeholder jika kosong
            txtNomorTelpon.setForeground(java.awt.Color.GRAY); // Warna placeholder
        }
    }//GEN-LAST:event_txtNomorTelponFocusLost

    private void txtCariFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtCariFocusGained
        if (txtCari.getText().equals("Cari Kontak")) {
            txtCari.setText(""); // Hapus placeholder
             txtCari.setForeground(java.awt.Color.BLACK); // Warna teks input
        }
    }//GEN-LAST:event_txtCariFocusGained

    private void txtCariFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtCariFocusLost
        if (txtCari.getText().isEmpty()) {
            txtCari.setText("Cari Kontak"); // Kembalikan placeholder jika kosong
            txtCari.setForeground(java.awt.Color.GRAY); // Warna placeholder
        }
    }//GEN-LAST:event_txtCariFocusLost

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PengelolaanKontakForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PengelolaanKontakForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PengelolaanKontakForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PengelolaanKontakForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PengelolaanKontakForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnExport;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnImport;
    private javax.swing.JButton btnTambah;
    private javax.swing.JComboBox<String> cmbKategori;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblKategori;
    private javax.swing.JLabel lblNama;
    private javax.swing.JLabel lblNomor;
    private javax.swing.JTable tblDaftarKontak;
    private javax.swing.JTextField txtCari;
    private javax.swing.JTextField txtNama;
    private javax.swing.JTextField txtNomorTelpon;
    // End of variables declaration//GEN-END:variables
}
