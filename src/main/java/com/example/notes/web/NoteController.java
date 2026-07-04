package com.example.notes.web;

import com.example.notes.model.Note;
import com.example.notes.model.User;
import com.example.notes.repo.NoteRepository;
import com.example.notes.repo.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

@Controller
public class NoteController {

    private final NoteRepository notes;
    private final UserRepository users;

    @PersistenceContext
    private EntityManager em;

    public NoteController(NoteRepository notes, UserRepository users) {
        this.notes = notes; this.users = users;
    }

    private User current(Principal principal) {
        return users.findByEmail(principal.getName()).orElseThrow();
    }

    @GetMapping("/notes")
    public String index(Principal principal, Model model) {
        model.addAttribute("notes", notes.findByOwnerId(current(principal).getId()));
        return "notes/index";
    }

    @GetMapping("/notes/{id}")
    public String view(@PathVariable Long id, Model model) {
        // [OWASP A01 - IDOR] pobranie po samym id, BEZ weryfikacji wlasciciela - celowo w wariancie bazowym
        Note note = notes.findById(id).orElseThrow();
        model.addAttribute("note", note);
        return "notes/view";
    }

    @GetMapping("/notes/new")
    public String createForm() { return "notes/create"; }

    @PostMapping("/notes")
    public String create(@RequestParam String title, @RequestParam String body, Principal principal) {
        Note n = new Note();
        n.setOwner(current(principal));
        n.setTitle(title);
        n.setBody(body); // tresc bez sanityzacji; ekspozycja XSS zalezy od renderowania w widoku
        notes.save(n);
        return "redirect:/notes";
    }

    @GetMapping("/notes/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("note", notes.findById(id).orElseThrow()); // [OWASP A01] brak weryfikacji wlasciciela
        return "notes/edit";
    }

    @PostMapping("/notes/{id}/edit")
    public String edit(@PathVariable Long id, @RequestParam String title, @RequestParam String body) {
        Note n = notes.findById(id).orElseThrow(); // [OWASP A01] brak weryfikacji wlasciciela
        n.setTitle(title);
        n.setBody(body);
        n.setUpdatedAt(Instant.now());
        notes.save(n);
        return "redirect:/notes/" + id;
    }

    @PostMapping("/notes/{id}/delete")
    public String delete(@PathVariable Long id) {
        notes.deleteById(id); // [OWASP A01] brak weryfikacji wlasciciela
        return "redirect:/notes";
    }

    @GetMapping("/notes/search")
    @SuppressWarnings("unchecked")
    public String search(@RequestParam String q, Principal principal, Model model) {
        // [OWASP A05 - SQL Injection] naiwna konkatenacja do surowego SQL - celowo w wariancie bazowym.
        // Idiomatyczne zapytanie pochodne / @Query z parametrem byloby bezpieczne.
        Long uid = current(principal).getId();
        String sql = "SELECT * FROM notes WHERE owner_id = " + uid + " AND title ILIKE '%" + q + "%'";
        List<Note> result = em.createNativeQuery(sql, Note.class).getResultList();
        model.addAttribute("notes", result);
        model.addAttribute("query", q);
        return "notes/index";
    }

    @PostMapping("/notes/import")
    public String importUrl(@RequestParam String url, Principal principal) throws Exception {
        // [OWASP A01 - SSRF] serwer pobiera dowolny URL bez walidacji - celowo w wariancie bazowym
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Note n = new Note();
        n.setOwner(current(principal));
        n.setTitle("Import z " + url);
        n.setBody(resp.body());
        notes.save(n);
        return "redirect:/notes";
    }
}
