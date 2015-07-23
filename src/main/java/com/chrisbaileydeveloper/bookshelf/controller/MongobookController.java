package com.chrisbaileydeveloper.bookshelf.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.chrisbaileydeveloper.bookshelf.domain.Mongobook;
import com.chrisbaileydeveloper.bookshelf.service.MongobookService;
import com.chrisbaileydeveloper.bookshelf.web.util.ImageUtil;
import com.chrisbaileydeveloper.bookshelf.web.util.Message;
import com.chrisbaileydeveloper.bookshelf.web.util.UrlUtil;

@RequestMapping("/")
@Controller
public class MongobookController { 

	final Logger logger = LoggerFactory.getLogger(MongobookController.class);

	@Autowired
	private MongobookService mongobookService;

	@Autowired
	private MessageSource messageSource;

	/**
	 * List all books.
     */
	@RequestMapping(method = RequestMethod.GET)
	public String list(Model model) {
		logger.info("Listing books");

		List<Mongobook> books = mongobookService.findAll();
		model.addAttribute("books", books);

		logger.info("No. of books: " + books.size());

		return "books/list";
	}

	/**
	 * Retrieve the book with the specified id.
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public String show(@PathVariable("id") String id, Model model) {
		logger.info("Listing book with id: " + id);

		Mongobook book = mongobookService.findById(id);
		model.addAttribute("book", book);
		
		return "books/show";
	}

	/**
	 * Retrieve the book with the specified id for the update form.
	 */
	@RequestMapping(value = "/update/{id}", method = RequestMethod.GET)
    public String updateForm(@PathVariable("id") String id, Model model) {
		model.addAttribute("book", mongobookService.findById(id));
        return "books/create";
    }

	/**
	 * Create a new book and place in Model attribute.
	 */
	@RequestMapping(value="/create", method=RequestMethod.GET)
    public String createForm(Model model) {
		model.addAttribute("book", new Mongobook());
        return "books/create";
    }

	/**
	 * Create/update a book.
	*/
	@RequestMapping(value="/create", method = RequestMethod.POST)
	public String create(@Valid Mongobook book, BindingResult bindingResult,
			Model model, HttpServletRequest httpServletRequest,
			RedirectAttributes redirectAttributes, Locale locale,
			@RequestParam(value = "file", required = false) MultipartFile file) {
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("book", book);
			return "books/create";
		}
		
		logger.info("Creating/updating book");
		
		model.asMap().clear();
		redirectAttributes.addFlashAttribute("message", new Message(
				"success", messageSource.getMessage("book_save_success", new Object[] {}, locale)));

		// Process upload file
		if (!file.isEmpty()
				&& (file.getContentType().equals(MediaType.IMAGE_JPEG_VALUE) || 
					file.getContentType().equals(MediaType.IMAGE_PNG_VALUE) ||
					file.getContentType().equals(MediaType.IMAGE_GIF_VALUE))) {
			
			logger.info("File name: " + file.getName());
			logger.info("File size: " + file.getSize());
			logger.info("File content type: " + file.getContentType());

			byte[] fileContent = null;
			String imageString = null;

			try {
				InputStream inputStream = file.getInputStream();
				fileContent = IOUtils.toByteArray(inputStream);

				// Convert byte[] into String image
				imageString = ImageUtil.encodeToString(fileContent);
				
				book.setPhoto(imageString);

			} catch (IOException ex) {
				logger.error("Error saving uploaded file");
				book.setPhoto(ImageUtil.smallNoImage());
			}
		} else { // File is improper type or no file was uploaded.

			// If book already exists, load its image into the 'book' object.
			if (book.getId() != null) {
				Mongobook savedMongobook = mongobookService.findById(book.getId());
				book.setPhoto(savedMongobook.getPhoto());

			} else {// Else set to default no-image picture.
				book.setPhoto(ImageUtil.smallNoImage());
			}
		}

		mongobookService.save(book);

		return "redirect:/" + UrlUtil.encodeUrlPathSegment(book.getId().toString(), httpServletRequest);
	}

	/**
	 * Returns the photo for the book with the specified id.
	 */
	@RequestMapping(value = "/photo/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	@ResponseBody
	public byte[] downloadPhoto(@PathVariable("id") String id) {

		Mongobook book = mongobookService.findById(id);
		logger.info("Downloading photo for id: {} with size: {}", book.getId(), book.getPhoto().length());

		// Convert String image into byte[]
		byte[] imageBytes = ImageUtil.decode(book.getPhoto());
		
		return imageBytes;
	}

	/**
	 * Deletes the book with the specified id.
	 */
	@RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
	public String delete(@PathVariable String id, Model model, Locale locale) {
		logger.info("Deleting book with id: " + id);
		Mongobook book = mongobookService.findById(id);

		if (book != null) {
			mongobookService.delete(book);
			logger.info("Mongobook deleted successfully");

			model.addAttribute("message",	new Message("success", messageSource.getMessage(
							"book_delete_success", new Object[] {}, locale)));
		}

		List<Mongobook> books = mongobookService.findAll();
		model.addAttribute("books", books);

		return "books/list";
	}
	
	
	// TODO -> Come back to this CJB 
	/*@RequestMapping(value="/reset", method=RequestMethod.GET)
	public String resetDatabase(Model model) {
		logger.info("Resetting database to original state");
		
		mongobookService.deleteAll();
		mongobookService.restoreDefaultMongobooks();
		
		List<Mongobook> books = mongobookService.findAll();
		model.addAttribute("books", books);

		return "books/list";
	}*/
}
