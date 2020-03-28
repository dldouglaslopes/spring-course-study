package com.douglas.springcoursestudy.resource;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.douglas.springcoursestudy.domain.Request;
import com.douglas.springcoursestudy.domain.User;
import com.douglas.springcoursestudy.dto.UserLoginDTO;
import com.douglas.springcoursestudy.dto.UserSaveDTO;
import com.douglas.springcoursestudy.dto.UserUpdateRoleDTO;
import com.douglas.springcoursestudy.model.PageModel;
import com.douglas.springcoursestudy.model.PageRequestModel;
import com.douglas.springcoursestudy.security.AccessManager;
import com.douglas.springcoursestudy.security.JwtManager;
import com.douglas.springcoursestudy.service.RequestService;
import com.douglas.springcoursestudy.service.UserService;

@RestController
@RequestMapping(value = "users")
public class UserResource {

	@Autowired
	private UserService userService;
	@Autowired
	private RequestService requestService;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private JwtManager jwtManager;
	@Autowired
	private AccessManager accessManager;
	
	@Secured({ "ROLE_ADMINISTRATOR" })
	@PostMapping
	public ResponseEntity<User> save(@RequestBody @Valid UserSaveDTO userDTO) {
		User createdUser = userService.save(userDTO.toUser());
		return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
	}
	
	@PreAuthorize("@accessManager.isOwner(#id)")
	@PutMapping("/{id}")
	public ResponseEntity<User> update(@PathVariable(name = "id") Long id,
										@RequestBody @Valid UserSaveDTO userDTO){
		User user = userDTO.toUser();
		user.setId(id);
		User updatedUser = userService.update(user);
		return ResponseEntity.ok(updatedUser);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<User> getById(@PathVariable("id") Long id) {
		User user = userService.getById(id);
		return ResponseEntity.ok(user);
	}
	
	@GetMapping
	public ResponseEntity<PageModel<User>> listAll(@RequestParam(value = "page", defaultValue = "0") int page,
													@RequestParam(value = "size", defaultValue = "10") int size) {
		PageRequestModel pr = new PageRequestModel(page, size);
		PageModel<User> pm = userService.listAllOnLazyMode(pr);
		return ResponseEntity.ok(pm);
	}
	
	@PostMapping("/login")
	public ResponseEntity<String> login(@RequestBody @Valid UserLoginDTO user) {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());
		Authentication auth = authenticationManager.authenticate(token);
		
		SecurityContextHolder.getContext().setAuthentication(auth);
		org.springframework.security.core.userdetails.User userSpring = 
				(org.springframework.security.core.userdetails.User) auth.getPrincipal(); 
				
		String email = userSpring.getUsername();
		List<String> roles = userSpring.getAuthorities().stream()
														.map(authority -> authority.getAuthority())
														.collect(Collectors.toList());
		
		String jwt = jwtManager.createToken(email, roles);
		
		return ResponseEntity.ok(jwt);
	}
	
	@GetMapping("/{id}/requests")
	public ResponseEntity<PageModel<Request>> listAllRequestsById(@PathVariable(name = "id") Long id,
															@RequestParam(value = "page", defaultValue = "0") int page,
															@RequestParam(value = "size", defaultValue = "10") int size) {
		PageRequestModel pr = new PageRequestModel(page, size);
		PageModel<Request> pm = requestService.listAllByOwnerIdOnLazyModel(id, pr);
		return ResponseEntity.ok(pm);
	}
	
	@Secured({ "ROLE_ADMINISTRATOR" })
	@PatchMapping("/role/{id}")
	public ResponseEntity<?> updateRole(@PathVariable(name = "id") Long id,
										@RequestBody @Valid UserUpdateRoleDTO userUpdateRoleDTO) {
		User user = new User();
		user.setId(id);
		user.setRole(userUpdateRoleDTO.getRole());
		
		userService.updateRole(user);
		
		return ResponseEntity.ok().build();
	}
	
}
