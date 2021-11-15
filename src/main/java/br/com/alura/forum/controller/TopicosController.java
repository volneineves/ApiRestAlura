package br.com.alura.forum.controller;

import br.com.alura.forum.dto.DetalhesTopicoDto;
import br.com.alura.forum.dto.TopicoDto;
import br.com.alura.forum.form.AtualizacaoTopicoForm;
import br.com.alura.forum.form.TopicoForm;
import br.com.alura.forum.modelo.Topico;
import br.com.alura.forum.repository.CursoRepository;
import br.com.alura.forum.repository.TopicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/topicos")
public class TopicosController {

    @Autowired
    private TopicoRepository topicoRepository;

    @Autowired
    private CursoRepository cursoRepository;

//    @GetMapping
//    public Page<TopicoDto> lista(@RequestParam(required = false) String nomeCurso, @RequestParam int pagina, @RequestParam int qtd, @RequestParam String ordernacao){ // Esse parâmetro deve ser passado igual ao nome da variável:http://localhost:8080/topicos?nomeCurso=Spring+Boot
//        Pageable paginacao = PageRequest.of(pagina, qtd, Sort.Direction.ASC, ordernacao);// O Sort.Direction.ASC pega o valor da ordenação para parâmetro de crescente

    @GetMapping
    @Cacheable(value = "listaDeTopicos")
    public Page<TopicoDto> lista(@RequestParam(required = false) String nomeCurso, @PageableDefault(sort = "id", direction = Sort.Direction.DESC, page = 0, size = 5) Pageable paginacao) { //Com a anotação @EnableSpringDataWebSupport na classe main é possível receber os parâmetros.Obs.: Agora os parâmetros são passados em inglês pois não criamos variáveis no nome em português
        //http://localhost:8080/topicos?page=0&size=10&sort=titulo,desc
        Page<Topico> topicos;
        if (nomeCurso == null) {
            topicos = topicoRepository.findAll(paginacao);
        } else {
            topicos = topicoRepository.findByCurso_Nome(nomeCurso, paginacao);
        }
        return TopicoDto.converter(topicos);

    }

    @PostMapping
    @Transactional // Avisar ao Spring pra comitar ao final do método
    @CacheEvict(value = "listaDeTopicos", allEntries = true) // O value do @CacheEvict deve ser igual ao cache que deseja atualizar, no caso, do método "lista"
    public ResponseEntity<TopicoDto> cadastrar(@RequestBody @Valid TopicoForm form, UriComponentsBuilder uriBuilder) {
        Topico topico = form.converter(cursoRepository);
        topicoRepository.save(topico);
        URI uri = uriBuilder.path("/topicos/{id}").buildAndExpand(topico.getId()).toUri();
        return ResponseEntity.created(uri).body(new TopicoDto(topico));
    }

    //Parâmetro dinâmico
    //Variável do Path, da url
    @GetMapping("/{id}")
    public ResponseEntity<DetalhesTopicoDto> detalhar(@PathVariable Long id) {

        Optional<Topico> topico = topicoRepository.findById(id); // findById retorna um Optional e não um Exception
        return topico.map(value -> ResponseEntity.ok(new DetalhesTopicoDto(value))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Transactional // Avisar ao Spring pra comitar ao final do método
    @CacheEvict(value = "listaDeTopicos", allEntries = true) // O value do @CacheEvict deve ser igual ao cache que deseja atualizar, no caso, do método "lista"
    public ResponseEntity<TopicoDto> atualizar(@PathVariable Long id, @RequestBody @Valid AtualizacaoTopicoForm form) {
        Optional<Topico> optional = topicoRepository.findById(id);

        return optional.map(x -> {
            Topico topico = form.atualizar(id, topicoRepository);
            return ResponseEntity.ok(new TopicoDto(topico));
        }).orElseGet(() ->  ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional // Avisar ao Spring pra comitar ao final do método
    @CacheEvict(value = "listaDeTopicos", allEntries = true) // O value do @CacheEvict deve ser igual ao cache que deseja atualizar, no caso, do método "lista"
    public ResponseEntity<?> remover(@PathVariable Long id) {
        Optional<Topico> optional = topicoRepository.findById(id);

       return optional.map(value -> {
           topicoRepository.deleteById(id);
           return ResponseEntity.ok().build();
       }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
