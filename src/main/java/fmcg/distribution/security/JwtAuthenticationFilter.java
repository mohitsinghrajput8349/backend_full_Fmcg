@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain chain)
        throws ServletException, IOException {

    String path = request.getRequestURI();

    // ✅ Allow CORS preflight
    if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
        chain.doFilter(request, response);
        return;
    }

    // ✅ Skip JWT for public endpoints
    if (path.startsWith("/api/auth") || path.startsWith("/api/files")) {
        chain.doFilter(request, response);
        return;
    }

    final String authorizationHeader = request.getHeader("Authorization");

    String email = null;
    String jwt = null;

    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
        jwt = authorizationHeader.substring(7);
        try {
            email = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            logger.error("JWT extraction error: " + e.getMessage());
        }
    }

    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (jwtUtil.validateToken(jwt, userDetails)) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }

    chain.doFilter(request, response);
}
