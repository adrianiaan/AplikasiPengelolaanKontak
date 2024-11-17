# Aplikasi Pengelolaan Kontak
Latihan 3 - Adrian Akhmad Firdaus (2210010491)  

---

## Deskripsi 

Aplikasi desktop **Pengelolaan Kontak** adalah sistem manajemen kontak yang dibangun menggunakan **Java Swing** dan **SQLite** untuk mempermudah pengelolaan kontak pribadi maupun profesional. Aplikasi ini dirancang untuk memenuhi kebutuhan pengguna dalam menyimpan, mencari, dan memodifikasi informasi kontak secara efisien.

---

## Fitur Utama

1. **CRUD (Create, Read, Update, Delete):**
   - Menambahkan kontak baru.
   - Menampilkan daftar kontak dalam tabel.
   - Mengedit kontak yang dipilih.
   - Menghapus kontak berdasarkan ID.

2. **Pencarian Dinamis:**
   - Mencari kontak berdasarkan nama, nomor telepon, atau kategori.
   - Menggunakan input teks dengan placeholder.

3. **Import dan Export CSV:**
   - **Import:** Memasukkan data dari file CSV dengan validasi format.
   - **Export:** Menyimpan data kontak ke file CSV dengan header `Nama, Nomor Telepon, Kategori`.

4. **Validasi Input:**
   - Nomor telepon hanya dapat diisi angka.
   - Nama hanya dapat diisi dengan huruf dan spasi.
   - Menampilkan pesan kesalahan jika input tidak valid.

5. **Placeholder Input:**
   - Input field memiliki placeholder seperti `Masukkan Nama`, `Masukkan Nomor Telepon`, dan `Cari Kontak`.

6. **Penyimpanan Data:**
   - Data disimpan menggunakan SQLite dengan tabel `kontak`:
     - `id` (INTEGER, PRIMARY KEY, AUTO_INCREMENT)
     - `nama` (TEXT)
     - `nomor_telepon` (TEXT)
     - `kategori` (TEXT)

---

## Struktur Kode Penting

### **1. Membuat Tabel Database** 
Kode untuk memastikan database SQLite memiliki tabel `kontak`:
```java
private void buatDatabase() {
    String sqlCreateTable = "CREATE TABLE IF NOT EXISTS kontak ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "nama TEXT NOT NULL,"
            + "nomor_telepon TEXT NOT NULL,"
            + "kategori TEXT NOT NULL"
            + ");";

    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:kontak.db");
         Statement stmt = conn.createStatement()) {
        stmt.execute(sqlCreateTable);
        System.out.println("Database dan tabel berhasil dibuat.");
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
    }
}
```

---

### **2. CRUD (Logika CRUD)**

#### a. Menambah Kontak
```java
private void tambahKontak(String nama, String nomorTelepon, String kategori) {
    String sql = "INSERT INTO kontak(nama, nomor_telepon, kategori) VALUES(?,?,?)";
    try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nama);
        pstmt.setString(2, nomorTelepon);
        pstmt.setString(3, kategori);
        pstmt.executeUpdate();
        JOptionPane.showMessageDialog(this, "Kontak berhasil ditambahkan.");
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Gagal menambahkan kontak: " + e.getMessage());
    }
}
```

#### b. Membaca dan Menampilkan Data
```java
private void tampilkanKontak() {
    model.setRowCount(0);
    String sql = "SELECT * FROM kontak";
    try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            model.addRow(new Object[]{rs.getInt("id"), rs.getString("nama"), rs.getString("nomor_telepon"), rs.getString("kategori")});
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Gagal menampilkan kontak: " + e.getMessage());
    }
}
```

#### c. Mengupdate Kontak
```java
private void editKontak(int id, String nama, String nomorTelepon, String kategori) {
    String sql = "UPDATE kontak SET nama = ?, nomor_telepon = ?, kategori = ? WHERE id = ?";
    try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
```

#### d. Menghapus Kontak
```java
private void hapusKontak(int id) {
    String sql = "DELETE FROM kontak WHERE id = ?";
    try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
        JOptionPane.showMessageDialog(this, "Kontak berhasil dihapus.");
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Gagal menghapus kontak: " + e.getMessage());
    }
}
```

---

### **3. Pencarian**
```java
private void cariKontak(String keyword) {
    model.setRowCount(0);
    String sql = "SELECT * FROM kontak WHERE nama LIKE ? OR nomor_telepon LIKE ? OR kategori LIKE ?";
    try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, "%" + keyword + "%");
        pstmt.setString(2, "%" + keyword + "%");
        pstmt.setString(3, "%" + keyword + "%");
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            model.addRow(new Object[]{rs.getInt("id"), rs.getString("nama"), rs.getString("nomor_telepon"), rs.getString("kategori")});
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Gagal mencari kontak: " + e.getMessage());
    }
}
```

---

### **4. Import dan Export CSV**

#### a. Import
```java
btnImport.addActionListener(e -> {
    JFileChooser fileChooser = new JFileChooser();
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine(); // Validasi header CSV
            if (header == null || !header.equals("Nama,Nomor Telepon,Kategori")) {
                JOptionPane.showMessageDialog(this, "Format CSV tidak valid.");
                return;
            }
            br.lines().forEach(line -> {
                String[] data = line.split(",");
                if (data.length == 3) tambahKontak(data[0].trim(), data[1].trim(), data[2].trim());
            });
            tampilkanKontak();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Gagal membaca file: " + ex.getMessage());
        }
    }
});
```

#### b. Export
```java
btnExport.addActionListener(e -> {
    JFileChooser fileChooser = new JFileChooser();
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("Nama,Nomor Telepon,Kategori");
            bw.newLine();
            for (int i = 0; i < model.getRowCount(); i++) {
                String row = model.getValueAt(i, 1) + "," + model.getValueAt(i, 2) + "," + model.getValueAt(i, 3);
                bw.write(row);
                bw.newLine();
            }
            JOptionPane.showMessageDialog(this, "Data berhasil diekspor.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan file: " + ex.getMessage());
        }
    }
});
```

---

## Cara Menjalankan
1. Clone repository:  
   ```bash
   git clone https://github.com/username/repository-name.git
   ```
2. Buka project di IDE (misalnya, NetBeans atau IntelliJ).
3. Jalankan aplikasi.

---

## Tampilan Aplikasi Pada saat Dijalankan

![image](https://github.com/user-attachments/assets/ee67e181-1083-4fe4-a675-42a3f5ca0a2c)


## Tampilan Data Dari Simpan (Eksport) .CSV
![image](https://github.com/user-attachments/assets/1c8e234d-4e86-46bc-ad6b-c9b9e39a6976)


---
## Indikator Penilaian  

| No  | Komponen           | Persentase |
|-----|---------------------|------------|
| 1   | Komponen GUI       | 20%        |
| 2   | Logika Program     | 30%        |
| 3   | Events             | 15%        |
| 4   | Kesesuaian UI      | 15%        |
| 5   | Memenuhi Variasi   | 20%        |
| **TOTAL** |               | **100%**   |  

---

## Pembuat  

- **Nama**: Adrian Akhmad Firdaus  
- **NPM**: 2210010491  
- **Kelas**: 5A Reguler Pagi  
- **Tugas**: Tugas 6 - Aplikasi Cek Cuaca  
- **Fakultas**: Fakultas Teknologi Informasi (FTI)  
- **Universitas**: Universitas Islam Kalimantan Muhammad Arsyad Al Banjari Banjarmasin  
