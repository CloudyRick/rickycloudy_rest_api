ALTER TABLE blog_images DROP FOREIGN KEY blog_images_ibfk_1;
ALTER TABLE blog_images
    ADD FOREIGN KEY (blog_post_id) REFERENCES blog_posts(id);